package com.minehost.app.server.java

import android.content.Context
import com.minehost.app.core.common.MinecraftEdition
import com.minehost.app.core.logging.AppLogger
import com.minehost.app.server.ManagedServerProcess
import com.minehost.app.server.ProcessListener
import com.minehost.app.server.RuntimeManager
import com.minehost.app.server.ServerConfiguration
import com.minehost.app.server.ServerEngine
import com.minehost.app.server.ServerProcessHandle
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import java.io.File
import java.io.FileNotFoundException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JavaEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val runtimeManager: RuntimeManager
) : ServerEngine {

    override val id: String = "java-paper"
    override val name: String = "PaperMC (Java Edition)"
    override val edition: MinecraftEdition = MinecraftEdition.JAVA
    override val defaultVersion: String = "1.20.4"
    override val supportedVersions: List<String> = listOf("1.20.4", "1.20.6", "1.21.0")

    private val scope = CoroutineScope(Dispatchers.IO)

    override fun isInstalled(): Boolean {
        return runtimeManager.isPaperJavaInstalled()
    }

    override suspend fun install(onProgress: (Float, String) -> Unit): Result<Unit> {
        AppLogger.i("JavaEngine", "Installing PaperMC server engine...")
        if (!runtimeManager.isSupportedArchitecture()) {
            return Result.failure(
                UnsupportedOperationException(
                    "Device architecture (${runtimeManager.getDeviceArchitecture()}) is not supported. MineHost requires 64-bit ARM (arm64-v8a)."
                )
            )
        }

        val result = runtimeManager.downloadPaperJar(defaultVersion, onProgress)
        return result.map { }
    }

    override suspend fun configure(serverDir: File, config: ServerConfiguration): Result<Unit> {
        return runCatching {
            JavaConfigGenerator.writeConfigurations(serverDir, config, config.eulaAccepted)
            AppLogger.i("JavaEngine", "Java Paper configured at: ${serverDir.absolutePath}")
        }
    }

    override suspend fun start(
        serverDir: File,
        config: ServerConfiguration,
        listener: ProcessListener
    ): Result<ServerProcessHandle> {
        val javaHome = runtimeManager.getJavaHome()
        val javaBinary = runtimeManager.getJavaExecutable()
        val jarFile = runtimeManager.getPaperJar()

        if (jarFile == null || !jarFile.exists()) {
            val errorMsg = "PaperMC server JAR was not found. Please install PaperMC from Server Settings or Setup."
            AppLogger.e("JavaEngine", errorMsg)
            listener.onError(errorMsg)
            return Result.failure(FileNotFoundException(errorMsg))
        }

        if (javaHome == null || javaBinary == null || !javaBinary.exists()) {
            val errorMsg = "A complete Java 17/21 runtime was not found. MineHost requires a tested Android ARM64 runtime with a bin/java launcher and its full library layout; a libjava.so file alone is not sufficient."
            AppLogger.e("JavaEngine", errorMsg)
            listener.onError(errorMsg)
            return Result.failure(FileNotFoundException(errorMsg))
        }

        val command = listOf(
            javaBinary.absolutePath,
            "-Xms${config.minMemoryMb}M",
            "-Xmx${config.maxMemoryMb}M",
            "-XX:+UseG1GC",
            "-jar",
            jarFile.absolutePath,
            "--nogui"
        )

        val javaLibDirs = listOf(
            File(javaHome, "lib"),
            File(javaHome, "lib/server")
        ).filter { it.isDirectory }.joinToString(":") { it.absolutePath }
        val env = mapOf(
            "JAVA_HOME" to javaHome.absolutePath,
            "PATH" to "${File(javaHome, "bin").absolutePath}:${System.getenv("PATH").orEmpty()}",
            "LD_LIBRARY_PATH" to javaLibDirs,
            "TMPDIR" to context.cacheDir.absolutePath
        )

        val managedProcess = ManagedServerProcess(
            command = command,
            workingDir = serverDir,
            environment = env,
            port = config.localPort,
            isUdp = false,
            listener = listener,
            scope = scope
        )

        return managedProcess.launchProcess().map { managedProcess }
    }

    override suspend fun stop(handle: ServerProcessHandle, timeoutMs: Long): Boolean {
        return if (handle is ManagedServerProcess) {
            handle.gracefulStop(timeoutMs)
        } else {
            handle.destroy()
            true
        }
    }

    override fun sendCommand(handle: ServerProcessHandle, command: String): Boolean {
        return if (handle is ManagedServerProcess) {
            handle.sendCommand(command)
        } else {
            false
        }
    }
}
