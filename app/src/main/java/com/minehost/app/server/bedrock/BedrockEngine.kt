package com.minehost.app.server.bedrock

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
class BedrockEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val runtimeManager: RuntimeManager
) : ServerEngine {

    override val id: String = "bedrock-pocketmine-v5"
    override val name: String = "PocketMine-MP (Bedrock)"
    override val edition: MinecraftEdition = MinecraftEdition.BEDROCK
    override val defaultVersion: String = "1.20.80"
    override val supportedVersions: List<String> = listOf("1.20.80", "1.21.0")

    private val scope = CoroutineScope(Dispatchers.IO)

    override fun isInstalled(): Boolean {
        return runtimeManager.isBedrockPocketMineInstalled()
    }

    override suspend fun install(onProgress: (Float, String) -> Unit): Result<Unit> {
        AppLogger.i("BedrockEngine", "Installing PocketMine-MP engine...")
        if (!runtimeManager.isSupportedArchitecture()) {
            return Result.failure(
                UnsupportedOperationException(
                    "Device architecture (${runtimeManager.getDeviceArchitecture()}) is not supported. MineHost requires a 64-bit ARM device (arm64-v8a)."
                )
            )
        }

        val result = runtimeManager.downloadPocketMinePhar(onProgress)
        return result.map { }
    }

    override suspend fun configure(serverDir: File, config: ServerConfiguration): Result<Unit> {
        return runCatching {
            BedrockConfigGenerator.writeConfigurations(serverDir, config)
            AppLogger.i("BedrockEngine", "Bedrock server configured at: ${serverDir.absolutePath}")
        }
    }

    override suspend fun start(
        serverDir: File,
        config: ServerConfiguration,
        listener: ProcessListener
    ): Result<ServerProcessHandle> {
        val phpBinary = runtimeManager.getPhpExecutable()
        val phar = runtimeManager.getPocketMinePhar()

        if (phpBinary == null || !phpBinary.exists()) {
            val errorMsg = "A runnable Android ARM64 PHP CLI runtime was not found. The Bedrock engine package alone is insufficient; install a compatible PHP runtime and launcher before starting the server."
            AppLogger.e("BedrockEngine", errorMsg)
            listener.onError(errorMsg)
            return Result.failure(FileNotFoundException(errorMsg))
        }

        if (phar == null || !phar.exists()) {
            val errorMsg = "PocketMine-MP.phar was not found in engines directory. Please download the Bedrock engine."
            AppLogger.e("BedrockEngine", errorMsg)
            listener.onError(errorMsg)
            return Result.failure(FileNotFoundException(errorMsg))
        }

        val command = listOf(
            phpBinary.absolutePath,
            phar.absolutePath,
            "--no-wizard",
            "--enable-ansi=false",
            "--data=${serverDir.absolutePath}"
        )

        val env = mutableMapOf(
            "LD_LIBRARY_PATH" to context.applicationInfo.nativeLibraryDir,
            "TMPDIR" to context.cacheDir.absolutePath
        )

        val managedProcess = ManagedServerProcess(
            command = command,
            workingDir = serverDir,
            environment = env,
            port = config.localPort,
            isUdp = true,
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
