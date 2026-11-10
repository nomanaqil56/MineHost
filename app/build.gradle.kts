import java.util.Properties
import java.net.URL
import java.io.DataInputStream
import java.security.MessageDigest

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.minehost.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.minehost.app"
        minSdk = 28
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        ndk {
            abiFilters.addAll(listOf("arm64-v8a", "x86_64"))
        }
    }

    // Release signing is read from environment variables or local.properties (never committed):
    //   MINEHOST_KEYSTORE_FILE, MINEHOST_KEYSTORE_PASSWORD, MINEHOST_KEY_ALIAS, MINEHOST_KEY_PASSWORD
    val localProps = Properties().apply {
        val f = rootProject.file("local.properties")
        if (f.exists()) f.inputStream().use { load(it) }
    }
    fun signingValue(name: String): String? = System.getenv(name) ?: localProps.getProperty(name)

    val releaseKeystorePath = signingValue("MINEHOST_KEYSTORE_FILE")
    val hasReleaseSigning = releaseKeystorePath != null &&
        file(releaseKeystorePath).exists() &&
        listOf("MINEHOST_KEYSTORE_PASSWORD", "MINEHOST_KEY_ALIAS", "MINEHOST_KEY_PASSWORD")
            .all { signingValue(it) != null }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = file(releaseKeystorePath!!)
                storePassword = signingValue("MINEHOST_KEYSTORE_PASSWORD")
                keyAlias = signingValue("MINEHOST_KEY_ALIAS")
                keyPassword = signingValue("MINEHOST_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Never fall back to the debug key: without a release key the APK is left UNSIGNED
            // (not installable), which is the honest result.
            signingConfig = if (hasReleaseSigning) signingConfigs.getByName("release") else null
            if (!hasReleaseSigning) {
                logger.warn("MineHost: no release signing key configured; the release build will be UNSIGNED.")
            }
        }
        debug {
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf(
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi"
        )
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.11"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
        jniLibs {
            // libphp.so is intentionally a statically linked AArch64 CLI executable,
            // not a shared library. Keep its bytes intact and extract it to nativeLibraryDir.
            useLegacyPackaging = true
            keepDebugSymbols += "**/libphp.so"
        }
    }
}


/*
 * The PHP CLI is fetched at BUILD TIME, checksum-verified, and embedded in the APK's
 * arm64-v8a native-library directory. Android 10+ blocks execve() from writable app
 * data directories, and Play distribution must not fetch executable native code at
 * runtime. Required PocketMine extensions are compiled into this static CLI; MineHost
 * uses PHP -n to avoid loading external modules referenced by the upstream php.ini.
 */
val phpRuntimeReleaseTag = "pm5-php-8.4-latest"
val phpRuntimeAssetName = "PHP-8.4-Android-arm64-PM5.tar.gz"
val phpRuntimeSha256 = "45b462ff5e290ee06d34eced589c325d27a2743185acab53acab291b0188e181"
val phpRuntimeDownloadUrl =
    "https://github.com/axolotl-pm/PHP-Binaries/releases/download/$phpRuntimeReleaseTag/$phpRuntimeAssetName"
val phpRuntimeArchive = layout.buildDirectory.file("downloads/$phpRuntimeAssetName").get().asFile
val phpRuntimeGeneratedRoot = layout.buildDirectory.dir("generated/minehost-php-runtime").get().asFile
val phpRuntimeJniLibs = File(phpRuntimeGeneratedRoot, "jniLibs")

fun sha256Hex(file: File): String {
    val digest = MessageDigest.getInstance("SHA-256")
    file.inputStream().use { input ->
        val buffer = ByteArray(64 * 1024)
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            digest.update(buffer, 0, count)
        }
    }
    return digest.digest().joinToString("") { "%02x".format(it) }
}

val prepareBundledPhpRuntime by tasks.registering {
    group = "build setup"
    description = "Downloads, verifies, and embeds the pinned Android ARM64 PHP CLI in the APK."
    inputs.property("phpRuntimeReleaseTag", phpRuntimeReleaseTag)
    inputs.property("phpRuntimeSha256", phpRuntimeSha256)
    outputs.dir(phpRuntimeGeneratedRoot)

    doLast {
        phpRuntimeArchive.parentFile.mkdirs()
        if (!phpRuntimeArchive.isFile || sha256Hex(phpRuntimeArchive) != phpRuntimeSha256) {
            val temporaryArchive = File(phpRuntimeArchive.parentFile, "${phpRuntimeArchive.name}.part")
            temporaryArchive.delete()
            val connection = URL(phpRuntimeDownloadUrl).openConnection().apply {
                connectTimeout = 30_000
                readTimeout = 120_000
                setRequestProperty("User-Agent", "MineHost-Android-Build/1.0")
            }
            connection.getInputStream().use { input ->
                temporaryArchive.outputStream().use { output -> input.copyTo(output) }
            }
            val downloadedSha256 = sha256Hex(temporaryArchive)
            if (downloadedSha256 != phpRuntimeSha256) {
                temporaryArchive.delete()
                throw GradleException(
                    "PHP runtime checksum mismatch: expected $phpRuntimeSha256, got $downloadedSha256"
                )
            }
            if (phpRuntimeArchive.exists() && !phpRuntimeArchive.delete()) {
                throw GradleException("Could not replace the cached PHP runtime archive")
            }
            if (!temporaryArchive.renameTo(phpRuntimeArchive)) {
                temporaryArchive.copyTo(phpRuntimeArchive, overwrite = true)
                temporaryArchive.delete()
            }
        }

        if (sha256Hex(phpRuntimeArchive) != phpRuntimeSha256) {
            throw GradleException("Cached PHP runtime archive failed SHA-256 verification")
        }

        val archiveTree = tarTree(resources.gzip(phpRuntimeArchive))
        val phpEntry = archiveTree.matching { include("bin/php7/bin/php") }.singleFile

        val packagedPhp = File(phpRuntimeJniLibs, "arm64-v8a/libphp.so")
        packagedPhp.parentFile.mkdirs()
        phpEntry.copyTo(packagedPhp, overwrite = true)
        if (!packagedPhp.setExecutable(true, false)) {
            throw GradleException("Could not mark the packaged PHP CLI executable")
        }
        if (packagedPhp.length() < 5L * 1024L * 1024L) {
            throw GradleException("Packaged PHP CLI is unexpectedly small")
        }

        val elfHeader = ByteArray(20)
        DataInputStream(packagedPhp.inputStream()).use { it.readFully(elfHeader) }
        val elfType = (elfHeader[16].toInt() and 0xff) or ((elfHeader[17].toInt() and 0xff) shl 8)
        val elfMachine = (elfHeader[18].toInt() and 0xff) or ((elfHeader[19].toInt() and 0xff) shl 8)
        val isElf64LittleEndian = elfHeader[0] == 0x7f.toByte() &&
            elfHeader[1] == 'E'.code.toByte() &&
            elfHeader[2] == 'L'.code.toByte() &&
            elfHeader[3] == 'F'.code.toByte() &&
            elfHeader[4] == 2.toByte() && elfHeader[5] == 1.toByte()
        if (!isElf64LittleEndian || elfType != 2 || elfMachine != 183) {
            throw GradleException("Bundled PHP CLI is not an ELF64 little-endian AArch64 executable")
        }

        logger.lifecycle(
            "Embedded PHP runtime: PHP 8.4 Android ARM64, SHA-256 verified, " +
                "${packagedPhp.length()} bytes at ${packagedPhp.relativeTo(phpRuntimeGeneratedRoot)}"
        )
    }
}

android.sourceSets.getByName("main").jniLibs.srcDir(phpRuntimeJniLibs)
tasks.named("preBuild").configure { dependsOn(prepareBundledPhpRuntime) }

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // DataStore & Security
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.security.crypto)

    // Hilt DI
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    // WorkManager
    implementation(libs.androidx.work.runtime.ktx)

    // Coroutines & Serialization
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)

    // Networking
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.truth)
    testImplementation(libs.kotlinx.coroutines.test)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}

