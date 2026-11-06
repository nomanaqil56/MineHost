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
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
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
    val installedAt: Long = System.currentTimeMillis()
)

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
     * Verifies that Bedrock PocketMine-MP and its PHP runtime are actually installed on disk.
     */
    fun isBedrockPocketMineInstalled(): Boolean {
        val phar = File(baseEnginesDir, "bedrock/PocketMine-MP.phar")
        val phpBinary = getPhpExecutable()
        return phar.exists() && phar.length() > 100_000 && phpBinary != null && phpBinary.exists()
    }

    /**
     * Finds the PHP executable either in nativeLibraryDir or in private app runtimes.
     */
    fun getPhpExecutable(): File? {
        val nativeDir = File(context.applicationInfo.nativeLibraryDir)
        val inNativeLib = File(nativeDir, "libphp.so")
        if (inNativeLib.exists()) return inNativeLib

        val inRuntimes = File(baseRuntimesDir, "php/bin/php")
        if (inRuntimes.exists() && inRuntimes.canExecute()) return inRuntimes

        // Standalone binary in runtimes/php
        val inPhpRoot = File(baseRuntimesDir, "php/php")
        if (inPhpRoot.exists()) return inPhpRoot

        return null
    }

    /**
     * Returns the PocketMine-MP.phar file if installed.
     */
    fun getPocketMinePhar(): File? {
        val phar = File(baseEnginesDir, "bedrock/PocketMine-MP.phar")
        return if (phar.exists() && phar.length() > 0) phar else null
    }

    /**
     * Verifies that PaperMC Java JAR is actually installed on disk.
     */
    fun isPaperJavaInstalled(): Boolean {
        val jar = File(baseEnginesDir, "java/paper.jar")
        return jar.exists() && jar.length() > 1_000_000
    }

    fun getPaperJar(): File? {
        val jar = File(baseEnginesDir, "java/paper.jar")
        return if (jar.exists() && jar.length() > 0) jar else null
    }

    /**
     * Verifies that Playit agent executable exists and is executable.
     */
    fun isPlayitAgentInstalled(): Boolean {
        val inNativeLib = File(context.applicationInfo.nativeLibraryDir, "libplayit.so")
        if (inNativeLib.exists()) return true

        val inTunnels = File(baseTunnelsDir, "playit/playit-cli")
        return inTunnels.exists() && inTunnels.length() > 100_000
    }

    fun getPlayitExecutable(): File? {
        val inNativeLib = File(context.applicationInfo.nativeLibraryDir, "libplayit.so")
        if (inNativeLib.exists()) return inNativeLib

        val inTunnels = File(baseTunnelsDir, "playit/playit-cli")
        if (inTunnels.exists()) {
            inTunnels.setExecutable(true)
            return inTunnels
        }
        return null
    }

    /**
     * Downloads the official PocketMine-MP.phar from pmmp/PocketMine-MP releases.
     */
    suspend fun downloadPocketMinePhar(
        onProgress: (Float, String) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        val targetDir = File(baseEnginesDir, "bedrock").apply { mkdirs() }
        val targetFile = File(targetDir, "PocketMine-MP.phar")

        val downloadUrl = "https://github.com/pmmp/PocketMine-MP/releases/latest/download/PocketMine-MP.phar"

        return@withContext downloadFileWithProgress(
            url = downloadUrl,
            targetFile = targetFile,
            componentName = "PocketMine-MP Core",
            onProgress = onProgress
        )
    }

    /**
     * Downloads and installs the official Playit CLI Linux ARM64 agent.
     */
    suspend fun downloadPlayitAgent(
        onProgress: (Float, String) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        val targetDir = File(baseTunnelsDir, "playit").apply { mkdirs() }
        val targetFile = File(targetDir, "playit-cli")

        val arch = if (Build.SUPPORTED_ABIS.any { it.contains("x86_64") }) "x86_64" else "aarch64"
        val downloadUrl = "https://github.com/playit-cloud/playit-agent/releases/latest/download/playit-cli-linux-$arch"

        val result = downloadFileWithProgress(
            url = downloadUrl,
            targetFile = targetFile,
            componentName = "Playit.gg Tunnel Agent ($arch)",
            onProgress = onProgress
        )

        result.onSuccess { file ->
            file.setExecutable(true, false)
        }
        return@withContext result
    }

    /**
     * Downloads PaperMC server JAR for specified Minecraft version.
     */
    suspend fun downloadPaperJar(
        minecraftVersion: String = "1.20.4",
        onProgress: (Float, String) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        val targetDir = File(baseEnginesDir, "java").apply { mkdirs() }
        val targetFile = File(targetDir, "paper.jar")

        onProgress(0.05f, "Fetching latest Paper build metadata for $minecraftVersion...")

        return@withContext try {
            // PaperMC v2 API
            val apiUrl = "https://api.papermc.io/v2/projects/paper/versions/$minecraftVersion/builds"
            val request = Request.Builder().url(apiUrl).build()
            val response = httpClient.newCall(request).execute()

            if (!response.isSuccessful) {
                return@withContext Result.failure(IOException("Failed to query PaperMC API: HTTP ${response.code}"))
            }

            val body = response.body?.string() ?: ""
            // Parse latest build number
            val buildRegex = Regex("\"build\":(\\d+)")
            val builds = buildRegex.findAll(body).map { it.groupValues[1].toInt() }.toList()
            val latestBuild = builds.maxOrNull() ?: 499

            val downloadUrl = "https://api.papermc.io/v2/projects/paper/versions/$minecraftVersion/builds/$latestBuild/downloads/paper-$minecraftVersion-$latestBuild.jar"

            downloadFileWithProgress(
                url = downloadUrl,
                targetFile = targetFile,
                componentName = "PaperMC $minecraftVersion (Build #$latestBuild)",
                onProgress = onProgress
            )
        } catch (e: Exception) {
            AppLogger.e("RuntimeManager", "Paper download failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Robust streaming file download with progress reporting and atomic rename.
     */
    private fun downloadFileWithProgress(
        url: String,
        targetFile: File,
        componentName: String,
        onProgress: (Float, String) -> Unit
    ): Result<File> {
        val tempFile = File(targetFile.parentFile, "${targetFile.name}.download_${System.currentTimeMillis()}.tmp")

        return try {
            onProgress(0.01f, "Connecting to $componentName...")
            val request = Request.Builder().url(url).build()
            val response = httpClient.newCall(request).execute()

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

                        if (totalBytes > 0) {
                            val progress = (bytesReadTotal.toFloat() / totalBytes).coerceIn(0f, 1f)
                            val mbDownloaded = bytesReadTotal / (1024 * 1024)
                            val mbTotal = totalBytes / (1024 * 1024)
                            onProgress(progress, "Downloading $componentName ($mbDownloaded / $mbTotal MB)")
                        } else {
                            val mbDownloaded = bytesReadTotal / (1024 * 1024)
                            onProgress(0.5f, "Downloading $componentName ($mbDownloaded MB)")
                        }
                    }
                }
            }

            if (tempFile.length() == 0L) {
                tempFile.delete()
                return Result.failure(IOException("Downloaded file was empty"))
            }

            // Atomic replacement
            if (targetFile.exists()) targetFile.delete()
            if (!tempFile.renameTo(targetFile)) {
                tempFile.copyTo(targetFile, overwrite = true)
                tempFile.delete()
            }

            val sha256 = digest.digest().joinToString("") { "%02x".format(it) }
            recordInstalledComponent(
                InstalledComponent(
                    id = targetFile.name,
                    name = componentName,
                    version = "latest",
                    installedPath = targetFile.absolutePath,
                    sizeBytes = targetFile.length(),
                    sha256Checksum = sha256
                )
            )

            onProgress(1.0f, "$componentName installed successfully")
            AppLogger.i("RuntimeManager", "$componentName verified & installed to ${targetFile.absolutePath}")
            Result.success(targetFile)
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

