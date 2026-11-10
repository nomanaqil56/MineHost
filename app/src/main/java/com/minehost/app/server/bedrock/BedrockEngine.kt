package com.minehost.app.server.bedrock

import android.content.Context
import com.minehost.app.core.common.MinecraftEdition
import com.minehost.app.core.logging.AppLogger
import com.minehost.app.server.BedrockRuntimeContract
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
    override val name: String = "Axolotl-PM (Bedrock)"
    override val edition: MinecraftEdition = MinecraftEdition.BEDROCK
    override val defaultVersion: String = "5.51.1"
    override val supportedVersions: List<String> = listOf("5.51.1")

    private val scope = CoroutineScope(Dispatchers.IO)

    override fun isInstalled(): Boolean {
        return runtimeManager.isBedrockPocketMineInstalled()
    }

    override suspend fun install(onProgress: (Float, String) -> Unit): Result<Unit> {
        AppLogger.i("BedrockEngine", "Installing Axolotl-PM Bedrock engine...")
        if (!runtimeManager.isBedrockArchitectureSupported()) {
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

        if (phpBinary == null || !phpBinary.isFile || !phpBinary.canExecute()) {
            val errorMsg = "The APK does not contain a valid packaged Android ARM64 PHP CLI. Reinstall an ARM64 MineHost build that includes the bundled Bedrock runtime."
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

        val pharIntegrity = runtimeManager.verifyPocketMinePhar(phar)
        if (pharIntegrity.isFailure) {
            val errorMsg = pharIntegrity.exceptionOrNull()?.message ?: "Axolotl-PM PHAR integrity verification failed."
            AppLogger.e("BedrockEngine", errorMsg, pharIntegrity.exceptionOrNull())
            listener.onError(errorMsg)
            return Result.failure(pharIntegrity.exceptionOrNull() ?: IllegalStateException(errorMsg))
        }

        val phpCheck = runtimeManager.verifyExecutable(
            phpBinary, BedrockRuntimeContract.PHP_NO_INI_FLAG, "-v"
        )
        if (phpCheck.isFailure) {
            val errorMsg = "The packaged PHP CLI failed its Android launch check: ${phpCheck.exceptionOrNull()?.message}"
            AppLogger.e("BedrockEngine", errorMsg, phpCheck.exceptionOrNull())
            listener.onError(errorMsg)
            return Result.failure(phpCheck.exceptionOrNull() ?: IllegalStateException(errorMsg))
        }

        // Use the compiled-in module set in the exact no-ini mode used by the server.
        // The upstream php.ini references external modules that are not packaged with
        // this static Android CLI and caused false missing-extension failures on device.
        val moduleCheck = runtimeManager.verifyExecutable(
            phpBinary, BedrockRuntimeContract.PHP_NO_INI_FLAG, "-m"
        )
        if (moduleCheck.isFailure) {
            val errorMsg = "PHP could not load the bundled configuration or enumerate extensions: ${moduleCheck.exceptionOrNull()?.message}"
            AppLogger.e("BedrockEngine", errorMsg, moduleCheck.exceptionOrNull())
            listener.onError(errorMsg)
            return Result.failure(moduleCheck.exceptionOrNull() ?: IllegalStateException(errorMsg))
        }
        val missingModules = BedrockRuntimeContract.missingRequiredModules(moduleCheck.getOrThrow())
        if (missingModules.isNotEmpty()) {
            val errorMsg = "The packaged PHP runtime is missing required Axolotl-PM extensions: ${missingModules.joinToString()}. Reinstall MineHost with the supported runtime."
            AppLogger.e("BedrockEngine", errorMsg)
            listener.onError(errorMsg)
            return Result.failure(IllegalStateException(errorMsg))
        }

        val command = listOf(phpBinary.absolutePath) +
            BedrockRuntimeContract.serverArguments(phar.absolutePath, serverDir.absolutePath)

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
