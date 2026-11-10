package com.minehost.app.server

import android.content.Context
import android.os.Build
import com.minehost.app.core.common.Constants
import com.minehost.app.core.common.Resource
import com.minehost.app.core.logging.AppLogger
import com.minehost.app.core.util.FileUtils
import com.minehost.app.core.util.ZipUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.DataInputStream
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class InstalledComponent(
    val id: String,
    val name: String,
    val version: String,
    val installedPath: String,
    val sizeBytes: Long,
    val sha256Checksum: String,
    val installedAt: Long = System.currentTimeMillis(),
    /** True only if the download was compared against a checksum published by the source. */
    val verified: Boolean = false
)

@Serializable
private data class GhAsset(
    val name: String,
    @SerialName("browser_download_url") val url: String,
    val digest: String? = null
)

@Serializable
private data class GhRelease(
    @SerialName("tag_name") val tagName: String,
    val assets: List<GhAsset> = emptyList()
)

private const val USER_AGENT = "MineHost/1.0 (+https://github.com/nomanaqil56/MineHost)"

@Serializable
data class RuntimeManifest(
    val components: MutableMap<String, InstalledComponent> = mutableMapOf()
)

@Singleton
class RuntimeManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            chain.proceed(chain.request().newBuilder().header("User-Agent", USER_AGENT).build())
        }
        .build()

    val baseEnginesDir: File by lazy {
        File(context.filesDir, "${Constants.DIR_MINEHOST}/${Constants.DIR_ENGINES}").apply { mkdirs() }
    }

    val baseRuntimesDir: File by lazy {
        File(context.filesDir, "${Constants.DIR_MINEHOST}/${Constants.DIR_RUNTIMES}").apply { mkdirs() }
    }

    val baseTunnelsDir: File by lazy {
        File(context.filesDir, "${Constants.DIR_MINEHOST}/${Constants.DIR_TUNNELS}").apply { mkdirs() }
    }

    private val manifestFile: File by lazy {
        File(context.filesDir, "${Constants.DIR_MINEHOST}/runtime_manifest.json")
    }

    /**
     * Checks whether device CPU architecture is 64-bit ARM (arm64-v8a) or x86_64.
     */
    fun isSupportedArchitecture(): Boolean {
        val abis = Build.SUPPORTED_ABIS
        return abis.any { it.contains("arm64") || it.contains("aarch64") || it.contains("x86_64") }
    }

    fun getDeviceArchitecture(): String {
        return Build.SUPPORTED_ABIS.firstOrNull() ?: "unknown"
    }

    /**
     * Verifies that the Axolotl-PM PHAR and its PHP CLI candidate are present on disk.
     * This is a presence check only; it does not prove Android will permit process launch.
     */
    fun isBedrockPocketMineInstalled(): Boolean {
        val phar = File(baseEnginesDir, "bedrock/PocketMine-MP.phar")
        val phpBinary = getPhpExecutable()
        return phar.isFile && phar.length() > 100_000 &&
            phpBinary?.let { it.isFile && it.canExecute() } == true
    }

    /**
     * Bedrock's PHP CLI is bundled in the APK as a static AArch64 ELF executable
     * named libphp.so and extracted by Android into nativeLibraryDir. It is not
     * a shared library: the build verifies ET_EXEC, AArch64 and the pinned digest.
     * Never accept a PHP executable from writable app data on Android 10+.
     */
    fun getPhpExecutable(): File? {
        if (!isBedrockArchitectureSupported()) return null
        val packagedPhp = File(context.applicationInfo.nativeLibraryDir, "libphp.so")
        return packagedPhp.takeIf { isPackagedArm64PhpExecutable(it) }
    }

    /** The currently pinned Bedrock PHP runtime is built for arm64-v8a only. */
    fun isBedrockArchitectureSupported(): Boolean {
        return Build.SUPPORTED_ABIS.any { it == "arm64-v8a" || it.contains("aarch64") }
    }

    private fun isPackagedArm64PhpExecutable(file: File): Boolean {
        if (!file.isFile || !file.canExecute() || file.length() < 5L * 1024L * 1024L) return false
        return runCatching {
            val header = ByteArray(20)
            DataInputStream(file.inputStream()).use { it.readFully(header) }
            val elfType = (header[16].toInt() and 0xff) or ((header[17].toInt() and 0xff) shl 8)
            val elfMachine = (header[18].toInt() and 0xff) or ((header[19].toInt() and 0xff) shl 8)
            header[0] == 0x7f.toByte() &&
                header[1] == 'E'.code.toByte() &&
                header[2] == 'L'.code.toByte() &&
                header[3] == 'F'.code.toByte() &&
                header[4] == 2.toByte() &&
                header[5] == 1.toByte() &&
                elfType == 2 && elfMachine == 183
        }.getOrDefault(false)
    }

    /** Copies the small, non-executable PHP configuration from APK assets to app data. */
    fun preparePhpIni(): Result<File> = runCatching {
        val target = File(baseRuntimesDir, "php/php.ini")
        target.parentFile?.mkdirs()
        val temporary = File(target.parentFile, "php.ini.part")
        context.assets.open("php/php.ini").use { input ->
            FileOutputStream(temporary).use { output -> input.copyTo(output) }
        }
        try {
            Files.move(temporary.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
        } catch (_: Exception) {
            Files.move(temporary.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
        target
    }

    /** Returns true only when a complete Java launcher candidate is present. */
    fun isPaperJavaInstalled(): Boolean {
        val jar = File(baseEnginesDir, "java/paper.jar")
        return jar.isFile && jar.length() > 1_000_000 && getJavaExecutable() != null
    }

    /**
     * Finds a Java CLI launcher. A libjava.so shared library by itself is not
     * a JVM distribution or an executable Java command.
     */
    fun getJavaExecutable(): File? {
        val candidates = listOf(
            File(baseRuntimesDir, "java/bin/java"),
            File(baseRuntimesDir, "java/java")
        )
        return candidates.firstOrNull { it.isFile && it.canExecute() }
    }

    /**
     * Returns the PocketMine-MP.phar file if installed.
     */
    fun getPocketMinePhar(): File? {
        val phar = File(baseEnginesDir, "bedrock/PocketMine-MP.phar")
        return if (phar.exists() && phar.length() > 0) phar else null
    }

    fun getPaperJar(): File? {
        val jar = File(baseEnginesDir, "java/paper.jar")
        return if (jar.exists() && jar.length() > 0) jar else null
    }

    /**
     * Verifies that the downloaded Playit CLI is a real executable file.
     * A JNI shared object (libplayit.so) is not a standalone CLI process.
     */
    fun isPlayitAgentInstalled(): Boolean {
        val executable = getPlayitExecutable()
        return executable != null && executable.isFile &&
            executable.length() > 100_000 && executable.canExecute()
    }

    fun getPlayitExecutable(): File? {
        val executable = File(baseTunnelsDir, "playit/playit-cli")
        return executable.takeIf { it.isFile && it.length() > 100_000 && it.canExecute() }
    }

    // ------------------------------------------------------------------ source resolution

    private fun fetchText(url: String): Result<String> {
        return try {
            httpClient.newCall(Request.Builder().url(url).build()).execute().use { response ->
                if (!response.isSuccessful) {
                    Result.failure(IOException("HTTP ${response.code} from $url"))
                } else {
                    Result.success(response.body?.string() ?: "")
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun githubRelease(repo: String, tag: String): Result<GhRelease> {
        return fetchText("https://api.github.com/repos/$repo/releases/tags/$tag")
            .mapCatching { json.decodeFromString<GhRelease>(it) }
    }

    private fun latestGithubRelease(repo: String): Result<GhRelease> {
        return fetchText("https://api.github.com/repos/$repo/releases/latest")
            .mapCatching { json.decodeFromString<GhRelease>(it) }
    }

    /** GitHub publishes asset checksums as "sha256:<64 hex>". Returns the hex, or null if absent/other algorithm. */
    private fun sha256FromDigest(digest: String?): String? {
        val hex = digest?.removePrefix("sha256:") ?: return null
        return hex.takeIf { it.length == 64 && it.all { c -> c.isDigit() || c in 'a'..'f' || c in 'A'..'F' } }?.lowercase()
    }

    /**
     * Downloads the Axolotl-PM PocketMine-MP-compatible server archive.
     * The original pmmp/PocketMine-MP repository was archived in July 2026;
     * Axolotl-PM is the actively maintained continuation. The download is checked
     * against the release asset's published SHA-256 digest when available.
     */
    suspend fun downloadPocketMinePhar(
        onProgress: (Float, String) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        val selectedRelease = "5.51.1"
        onProgress(0.02f, "Looking up Axolotl-PM $selectedRelease...")
        val release = githubRelease("axolotl-pm/PocketMine-MP", selectedRelease).getOrElse {
            return@withContext Result.failure(IOException("Could not query Axolotl-PM releases: ${it.message}", it))
        }
        val asset = release.assets.firstOrNull { it.name == "PocketMine-MP.phar" }
            ?: return@withContext Result.failure(IOException("Release ${release.tagName} has no PocketMine-MP.phar asset."))

        val targetDir = File(baseEnginesDir, "bedrock").apply { mkdirs() }
        downloadFileWithProgress(
            url = asset.url,
            targetFile = File(targetDir, "PocketMine-MP.phar"),
            componentName = "Axolotl-PM ${release.tagName}",
            onProgress = onProgress,
            expectedSha256 = sha256FromDigest(asset.digest),
            version = release.tagName
        )
    }

    /**
     * Downloads the official Playit agent for this device's CPU, resolving the real asset name from the
     * release metadata (never a guessed URL), then proves the file can actually be executed here.
     */
    suspend fun downloadPlayitAgent(
        onProgress: (Float, String) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        onProgress(0.02f, "Looking up the latest Playit agent release...")
        val release = latestGithubRelease("playit-cloud/playit-agent").getOrElse {
            return@withContext Result.failure(IOException("Could not query Playit agent releases: ${it.message}", it))
        }

        val isArm = Build.SUPPORTED_ABIS.any { it.contains("arm64") || it.contains("aarch64") }
        val archTokens = if (isArm) listOf("aarch64", "arm64") else listOf("amd64", "x86_64")
        val skipSuffixes = listOf(".deb", ".rpm", ".sig", ".asc", ".sha256", ".zip", ".tar", ".gz", ".msi", ".exe", ".pkg")
        val asset = release.assets.firstOrNull { a ->
            val n = a.name.lowercase()
            n.startsWith("playit") && n.contains("linux") &&
                archTokens.any { n.contains(it) } && skipSuffixes.none { n.endsWith(it) }
        } ?: return@withContext Result.failure(
            IOException(
                "Playit release ${release.tagName} has no Linux ${archTokens.first()} binary. " +
                    "Available assets: ${release.assets.joinToString { it.name }.ifBlank { "none" }}"
            )
        )

        val targetDir = File(baseTunnelsDir, "playit").apply { mkdirs() }
        val targetFile = File(targetDir, "playit-cli")
        val downloaded = downloadFileWithProgress(
            url = asset.url,
            targetFile = targetFile,
            componentName = "Playit agent ${release.tagName} (${asset.name})",
            onProgress = onProgress,
            expectedSha256 = sha256FromDigest(asset.digest),
            version = release.tagName
        )
        val file = downloaded.getOrElse { return@withContext Result.failure(it) }

        file.setExecutable(true, false)
        onProgress(0.99f, "Checking that the agent can run on this device...")
        val check = verifyExecutable(file, "--version")
        if (check.isFailure) {
            // Do not leave a non-runnable file behind that Diagnostics would report as "installed".
            file.delete()
            return@withContext Result.failure(check.exceptionOrNull() ?: IOException("Agent cannot be executed"))
        }
        onProgress(1.0f, "Playit agent verified to run on this device")
        Result.success(file)
    }

    /**
     * Downloads the latest STABLE Paper build for [minecraftVersion] from the PaperMC downloads service
     * (fill.papermc.io, v3) and checks it against the published SHA-256 when the service provides one.
     */
    suspend fun downloadPaperJar(
        minecraftVersion: String = "1.20.4",
        onProgress: (Float, String) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        onProgress(0.02f, "Fetching Paper build metadata for $minecraftVersion...")
        val body = fetchText("https://fill.papermc.io/v3/projects/paper/versions/$minecraftVersion/builds").getOrElse {
            return@withContext Result.failure(IOException("Could not query the PaperMC downloads service: ${it.message}", it))
        }

        return@withContext try {
            val builds = json.parseToJsonElement(body).jsonArray.map { it.jsonObject }
            val latestStable = builds
                .filter { it["channel"]?.jsonPrimitive?.contentOrNull.equals("STABLE", ignoreCase = true) }
                .maxByOrNull { it["id"]?.jsonPrimitive?.intOrNull ?: -1 }
                ?: return@withContext Result.failure(
                    IOException("PaperMC has no STABLE build for Minecraft $minecraftVersion.")
                )
            val buildId = latestStable["id"]?.jsonPrimitive?.intOrNull ?: 0
            val download = latestStable["downloads"]?.jsonObject?.get("server:default")?.jsonObject
                ?: return@withContext Result.failure(IOException("Paper build #$buildId has no server download."))
            val url = download["url"]?.jsonPrimitive?.contentOrNull
                ?: return@withContext Result.failure(IOException("Paper build #$buildId has no download URL."))
            val sha256 = download["checksums"]?.jsonObject?.get("sha256")?.jsonPrimitive?.contentOrNull?.lowercase()

            val targetDir = File(baseEnginesDir, "java").apply { mkdirs() }
            downloadFileWithProgress(
                url = url,
                targetFile = File(targetDir, "paper.jar"),
                componentName = "PaperMC $minecraftVersion (build #$buildId)",
                onProgress = onProgress,
                expectedSha256 = sha256,
                version = "$minecraftVersion-$buildId"
            )
        } catch (e: Exception) {
            AppLogger.e("RuntimeManager", "Paper metadata parsing failed: ${e.message}", e)
            Result.failure(IOException("Unexpected response from the PaperMC downloads service: ${e.message}", e))
        }
    }

    // ------------------------------------------------------------------ executability

    /**
     * Proves a binary can be launched and completes its smoke command successfully.
     * A successful process start with a non-zero exit code is not considered healthy.
     */
    fun verifyExecutable(binary: File, vararg args: String, timeoutSeconds: Long = 10): Result<String> {
        return try {
            val process = ProcessBuilder(listOf(binary.absolutePath) + args.toList())
                .redirectErrorStream(true)
                .start()
            if (!process.waitFor(timeoutSeconds, TimeUnit.SECONDS)) {
                process.destroyForcibly()
                Result.failure(IOException("${binary.name} started but did not exit within ${timeoutSeconds}s."))
            } else {
                val output = process.inputStream.bufferedReader().readText().take(300)
                val exitCode = process.exitValue()
                if (exitCode == 0) {
                    Result.success(output)
                } else {
                    Result.failure(IOException("${binary.name} exited with code $exitCode: $output"))
                }
            }
        } catch (e: IOException) {
            val msg = e.message.orEmpty()
            val explanation = when {
                "error=13" in msg || "Permission denied" in msg ->
                    "Android refused to execute the packaged ${binary.name} from nativeLibraryDir. " +
                        "This device's SELinux or executable-file policy may block this launch path."
                "error=8" in msg || "Exec format" in msg ->
                    "${binary.name} was built for a different CPU architecture than this device."
                "error=2" in msg || "No such file" in msg ->
                    "${binary.name} cannot start: the executable or a required runtime component is unavailable on this Android device."
                else -> "${binary.name} could not be launched: $msg"
            }
            Result.failure(IOException(explanation, e))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ------------------------------------------------------------------ download core

    /**
     * Streaming download with true progress, completeness check, optional SHA-256 verification and
     * an atomic move into place. A failed or unverifiable download never replaces a working install.
     */
    private fun downloadFileWithProgress(
        url: String,
        targetFile: File,
        componentName: String,
        onProgress: (Float, String) -> Unit,
        expectedSha256: String? = null,
        version: String = "unknown"
    ): Result<File> {
        val tempFile = File(targetFile.parentFile, "${targetFile.name}.download_${System.currentTimeMillis()}.tmp")

        return try {
            onProgress(0.01f, "Connecting to $componentName...")
            val request = Request.Builder().url(url).build()
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return Result.failure(IOException("Server returned HTTP ${response.code} for $url"))
                }
                val responseBody = response.body ?: return Result.failure(IOException("Empty response body"))
                val totalBytes = responseBody.contentLength()

                val digest = MessageDigest.getInstance("SHA-256")
                var bytesReadTotal = 0L

                responseBody.byteStream().use { input ->
                    FileOutputStream(tempFile).use { output ->
                        val buffer = ByteArray(16384)
                        var bytes: Int
                        while (input.read(buffer).also { bytes = it } != -1) {
                            output.write(buffer, 0, bytes)
                            digest.update(buffer, 0, bytes)
                            bytesReadTotal += bytes

                            val mbDownloaded = bytesReadTotal / (1024 * 1024)
                            if (totalBytes > 0) {
                                val progress = (bytesReadTotal.toFloat() / totalBytes).coerceIn(0f, 0.98f)
                                onProgress(progress, "Downloading $componentName ($mbDownloaded / ${totalBytes / (1024 * 1024)} MB)")
                            } else {
                                // Total size unknown: report the real byte count, not an invented percentage.
                                onProgress(0f, "Downloading $componentName ($mbDownloaded MB)")
                            }
                        }
                    }
                }

                if (tempFile.length() == 0L) {
                    tempFile.delete()
                    return Result.failure(IOException("Downloaded file was empty"))
                }
                if (totalBytes > 0 && bytesReadTotal != totalBytes) {
                    tempFile.delete()
                    return Result.failure(IOException("Incomplete download of $componentName: received $bytesReadTotal of $totalBytes bytes."))
                }

                val sha256 = digest.digest().joinToString("") { "%02x".format(it) }
                if (expectedSha256 != null && !sha256.equals(expectedSha256, ignoreCase = true)) {
                    tempFile.delete()
                    return Result.failure(
                        IOException("Checksum mismatch for $componentName: expected $expectedSha256 but got $sha256. The file was discarded.")
                    )
                }
                if (expectedSha256 == null) {
                    AppLogger.w("RuntimeManager", "$componentName: the source published no checksum; integrity relies on HTTPS only.")
                }

                // Atomic replacement: the previous working install stays until the new file is complete.
                try {
                    Files.move(tempFile.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
                } catch (e: Exception) {
                    Files.move(tempFile.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
                }

                recordInstalledComponent(
                    InstalledComponent(
                        id = targetFile.name,
                        name = componentName,
                        version = version,
                        installedPath = targetFile.absolutePath,
                        sizeBytes = targetFile.length(),
                        sha256Checksum = sha256,
                        verified = expectedSha256 != null
                    )
                )

                onProgress(1.0f, "$componentName installed" + if (expectedSha256 != null) " (checksum verified)" else " (no published checksum to verify)")
                AppLogger.i("RuntimeManager", "$componentName installed to ${targetFile.absolutePath} (verified=${expectedSha256 != null})")
                Result.success(targetFile)
            }
        } catch (e: Exception) {
            tempFile.delete()
            AppLogger.e("RuntimeManager", "Download failed for $componentName: ${e.message}", e)
            Result.failure(e)
        }
    }

    private fun recordInstalledComponent(component: InstalledComponent) {
        try {
            val manifest = loadManifest()
            manifest.components[component.id] = component
            manifestFile.writeText(json.encodeToString(manifest))
        } catch (e: Exception) {
            AppLogger.w("RuntimeManager", "Could not write runtime manifest: ${e.message}")
        }
    }

    fun loadManifest(): RuntimeManifest {
        return if (manifestFile.exists()) {
            runCatching {
                json.decodeFromString<RuntimeManifest>(manifestFile.readText())
            }.getOrDefault(RuntimeManifest())
        } else {
            RuntimeManifest()
        }
    }
}

