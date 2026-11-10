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

    override val id: String = "bedrock-pocketmine-v5" // Stable persisted ID; engine implementation is Axolotl-PM.
    override val name: String = "Axolotl-PM (Bedrock)"
    override val edition: MinecraftEdition = MinecraftEdition.BEDROCK
    override val defaultVersion: String = "1.26.60"
    override val supportedVersions: List<String> = listOf("1.26.60")

    private val scope = CoroutineScope(Dispatchers.IO)

    override fun isInstalled(): Boolean {
        return runtimeManager.isBedrockPocketMineInstalled()
    }

    override suspend fun install(onProgress: (Float, String) -> Unit): Result<Unit> {
        AppLogger.i("BedrockEngine", "Installing Axolotl-PM Bedrock engine...")
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
            AppLogger.i("BedrockEngine", "Axolotl-PM Bedrock server configured at: ${serverDir.absolutePath}")
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
            val errorMsg = "APK-packaged Android PHP runtime (libphp.so) was not found. Rebuild the app with tools/prepare_android_php_runtime.sh and install the resulting APK."
            AppLogger.e("BedrockEngine", errorMsg)
            listener.onError(errorMsg)
            return Result.failure(FileNotFoundException(errorMsg))
        }

        if (phar == null || !phar.exists()) {
            val errorMsg = "Axolotl-PM server archive (PocketMine-MP.phar) was not found. Download the Bedrock engine first."
            AppLogger.e("BedrockEngine", errorMsg)
            listener.onError(errorMsg)
            return Result.failure(FileNotFoundException(errorMsg))
        }

        // File presence is not proof that Android can execute the packaged ELF. Probe it
        // before creating the server process so incompatible packaging/ABI fails clearly.
        val runtimeProbe = runtimeManager.verifyExecutable(phpBinary, "-v")
        val runtimeOutput = runtimeProbe.getOrElse { error ->
            val errorMsg = "The packaged Android PHP runtime could not be executed: ${error.message}"
            AppLogger.e("BedrockEngine", errorMsg, error)
            listener.onError(errorMsg)
            return Result.failure(error)
        }
        if (!runtimeOutput.contains("PHP")) {
            val errorMsg = "The packaged PHP runtime launched but did not return a valid PHP version response."
            AppLogger.e("BedrockEngine", errorMsg)
            listener.onError(errorMsg)
            return Result.failure(IllegalStateException(errorMsg))
        }
        AppLogger.i("BedrockEngine", "Validated PHP runtime: ${runtimeOutput.lineSequence().firstOrNull().orEmpty()}")

        // Axolotl-PM's NetherNet transport requires axolotl-pm/ext-webrtc. Fail closed
        // instead of silently starting a RakNet-only server that current clients cannot join.
        val moduleProbe = runtimeManager.verifyExecutable(phpBinary, "-m")
        val modules = moduleProbe.getOrElse { error ->
            val errorMsg = "Could not inspect PHP runtime extensions: ${error.message}"
            AppLogger.e("BedrockEngine", errorMsg, error)
            listener.onError(errorMsg)
            return Result.failure(error)
        }
        if ("webrtc" !in modules.lineSequence().map { it.trim().lowercase() }.toSet()) {
            val errorMsg = "The packaged PHP runtime is missing ext-webrtc. NetherNet is required for current Bedrock clients."
            AppLogger.e("BedrockEngine", errorMsg)
            listener.onError(errorMsg)
            return Result.failure(IllegalStateException(errorMsg))
        }

        val command = listOf(
            phpBinary.absolutePath,
            "-d", "phar.readonly=0",
            "-d", "phar.require_hash=1",
            "-d", "zend.assertions=1",
            "-d", "memory_limit=${config.maxMemoryMb}M",
            phar.absolutePath,
            "--no-wizard",
            "--enable-ansi=false",
            "--data=${serverDir.absolutePath}"
        )

        val env = mutableMapOf(
            "LD_LIBRARY_PATH" to context.applicationInfo.nativeLibraryDir,
            "TMPDIR" to context.cacheDir.absolutePath,
            // PocketMine's bundled Android PHP build is intended to run without a host php.ini.
            // This mirrors PMMP's start.sh behaviour when using its bundled PHP runtime.
            "PHPRC" to ""
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
