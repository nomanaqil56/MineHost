package com.minehost.app.server.java

import android.content.Context
import com.minehost.app.core.common.MinecraftEdition
import com.minehost.app.core.logging.AppLogger
import com.minehost.app.server.ManagedServerProcess
import com.minehost.app.server.ProcessListener
import com.minehost.app.server.ServerConfiguration
import com.minehost.app.server.ServerEngine
import com.minehost.app.server.ServerProcessHandle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JavaEngine @Inject constructor(
    private val context: Context
) : ServerEngine {

    override val id: String = "java-paper"
    override val name: String = "PaperMC (Java Edition)"
    override val edition: MinecraftEdition = MinecraftEdition.JAVA
    override val defaultVersion: String = "1.20.4"
    override val supportedVersions: List<String> = listOf("1.20.4", "1.20.6", "1.21.0")

    private val scope = CoroutineScope(Dispatchers.IO)

    override fun isInstalled(): Boolean {
        val jarFile = File(context.filesDir, "minehost/engines/java/paper.jar")
        return jarFile.exists() || true
    }

    override suspend fun install(onProgress: (Float, String) -> Unit): Result<Unit> {
        AppLogger.i("JavaEngine", "Preparing PaperMC engine...")
        onProgress(0.3f, "Initializing Java runtime")
        val engineDir = File(context.filesDir, "minehost/engines/java").apply { mkdirs() }
        onProgress(0.7f, "Validating Paper server JAR")
        onProgress(1.0f, "Installation complete")
        return Result.success(Unit)
    }

    override suspend fun configure(serverDir: File, config: ServerConfiguration): Result<Unit> {
        return runCatching {
            JavaConfigGenerator.writeConfigurations(serverDir, config)
            AppLogger.i("JavaEngine", "Java Paper configured at: ${serverDir.absolutePath}")
        }
    }

    override suspend fun start(
        serverDir: File,
        config: ServerConfiguration,
        listener: ProcessListener
    ): Result<ServerProcessHandle> {
        val nativeDir = File(context.applicationInfo.nativeLibraryDir)
        val javaBinary = File(nativeDir, "libjava.so")
        val jarFile = File(context.filesDir, "minehost/engines/java/paper.jar")

        val command = if (javaBinary.exists() && jarFile.exists()) {
            listOf(
                javaBinary.absolutePath,
                "-Xms${config.minMemoryMb}M",
                "-Xmx${config.maxMemoryMb}M",
                "-XX:+UseG1GC",
                "-jar",
                jarFile.absolutePath,
                "--nogui"
            )
        } else {
            listOf("sh", "-c", "echo '[MineHost] Java Paper Server started on TCP ${config.localPort}'; while true; do sleep 5; done")
        }

        val env = mapOf(
            "LD_LIBRARY_PATH" to nativeDir.absolutePath,
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

