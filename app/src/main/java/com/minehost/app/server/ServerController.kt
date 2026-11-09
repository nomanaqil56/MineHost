package com.minehost.app.server

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.minehost.app.core.common.MinecraftEdition
import com.minehost.app.core.common.Resource
import com.minehost.app.core.common.ServerStatus
import com.minehost.app.core.logging.AppLogger
import com.minehost.app.domain.model.PublicEndpoint
import com.minehost.app.domain.model.ServerModel
import com.minehost.app.domain.repository.LogRepository
import com.minehost.app.domain.repository.ServerRepository
import com.minehost.app.networking.TunnelManager
import com.minehost.app.services.ServerForegroundService
import com.minehost.app.services.ThermalManager
import com.minehost.app.services.WakeLockManager
import com.minehost.app.world.WorldManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.net.DatagramSocket
import java.net.ServerSocket
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ServerController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val engineRegistry: ServerEngineRegistry,
    private val tunnelManager: TunnelManager,
    private val worldManager: WorldManager,
    private val serverRepository: ServerRepository,
    private val logRepository: LogRepository,
    private val wakeLockManager: WakeLockManager,
    private val thermalManager: ThermalManager
) {
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _activeServerId = MutableStateFlow<String?>(null)
    val activeServerId: StateFlow<String?> = _activeServerId.asStateFlow()

    private val _serverStatus = MutableStateFlow(ServerStatus.STOPPED)
    val serverStatus: StateFlow<ServerStatus> = _serverStatus.asStateFlow()

    private var activeProcessHandle: ServerProcessHandle? = null
    private var activeEngine: ServerEngine? = null

    suspend fun startServer(serverId: String): Resource<Unit> {
        val server = serverRepository.getServerById(serverId)
            ?: return Resource.Error("Server with ID $serverId not found")

        if (_serverStatus.value.isRunning) {
            return Resource.Error("Another server instance is already running")
        }

        return try {
            _activeServerId.value = serverId
            _serverStatus.value = ServerStatus.STARTING
            serverRepository.updateStatus(serverId, ServerStatus.STARTING)
            log(serverId, "[MineHost] Preparing to start ${server.name} (${server.edition})...")

            // 1. Select engine and verify installation
            val engine = if (server.edition == MinecraftEdition.CROSSPLAY) {
                null
            } else {
                engineRegistry.getEngine(server.engineId) ?: engineRegistry.getEngineForEdition(server.edition)
            }
            if (engine == null) {
                val unsupportedMsg = "Cross-play (Java + Bedrock) is not supported yet: it requires Paper and Geyser running together, which MineHost does not implement. Create a Bedrock or Java server instead."
                log(serverId, "[ERROR] $unsupportedMsg")
                handleServerStopped(serverId, ServerStatus.ERROR)
                return Resource.Error(unsupportedMsg)
            }
            activeEngine = engine

            if (!engine.isInstalled()) {
                val notInstalledMsg = "Server engine '${engine.name}' is not installed on this device. Please download and install it via Diagnostics or Setup."
                log(serverId, "[ERROR] $notInstalledMsg")
                handleServerStopped(serverId, ServerStatus.ERROR)
                return Resource.Error(notInstalledMsg)
            }

            // 2. Validate port conflict before starting
            val isUdp = server.edition == MinecraftEdition.BEDROCK
            if (isPortInUse(server.localPort, isUdp)) {
                val portConflictMsg = "Port ${server.localPort} (${if (isUdp) "UDP" else "TCP"}) is already bound by another service on this device. Please select a different port in server settings."
                log(serverId, "[ERROR] $portConflictMsg")
                handleServerStopped(serverId, ServerStatus.ERROR)
                return Resource.Error(portConflictMsg)
            }

            // 3. Acquire system wake locks and thermal listener
            wakeLockManager.acquireLocks("MineHost:${server.name}")
            thermalManager.startListening()

            // 4. Configure server files on disk
            val serverDir = worldManager.getServerDirectory(serverId)
            val serverConfig = ServerConfiguration(
                serverId = server.id,
                serverName = server.name,
                motd = server.description,
                edition = server.edition,
                localPort = server.localPort,
                maxPlayers = server.maxPlayers,
                viewDistance = server.viewDistance,
                simulationDistance = server.simulationDistance,
                gameMode = server.gameMode,
                difficulty = server.difficulty,
                pvpEnabled = server.pvpEnabled,
                onlineMode = server.onlineMode,
                minMemoryMb = server.minMemoryMb,
                maxMemoryMb = server.maxMemoryMb
            )

            log(serverId, "[MineHost] Writing engine configuration files...")
            val configureResult = engine.configure(serverDir, serverConfig)
            if (configureResult.isFailure) {
                val configError = configureResult.exceptionOrNull()?.message ?: "unknown error"
                val msg = "Could not write server configuration files: $configError"
                log(serverId, "[ERROR] $msg")
                handleServerStopped(serverId, ServerStatus.ERROR)
                return Resource.Error(msg)
            }

            // 5. Start foreground service
            startForegroundService(server.name, server.localPort)

            // 6. Launch real engine process
            val processListener = object : ProcessListener {
                override fun onLog(line: String) {
                    scope.launch { log(serverId, line) }
                }

                override fun onStarted() {
                    scope.launch {
                        _serverStatus.value = ServerStatus.ONLINE
                        serverRepository.updateStatus(serverId, ServerStatus.ONLINE)
                        log(serverId, "[MineHost] Server process confirmed ONLINE on port ${server.localPort}!")

                        // 7. Connect tunnel if enabled
                        if (server.autoStartTunnel) {
                            log(serverId, "[MineHost] Initiating worldwide reverse tunnel...")
                            val tunnelResult = tunnelManager.startTunnel(server.localPort, server.edition.defaultProtocol)
                            tunnelResult.onSuccess { endpoint ->
                                serverRepository.updatePublicEndpoint(serverId, endpoint)
                                log(serverId, "[MineHost] Tunnel online! Public endpoint: ${endpoint.fullAddress}")
                            }.onFailure { err ->
                                log(serverId, "[MineHost] Tunnel notice: ${err.message}. Server is running in LOCAL mode.")
                            }
                        }
                    }
                }

                override fun onStopped(exitCode: Int) {
                    scope.launch {
                        AppLogger.i("ServerController", "Server process exited with code: $exitCode")
                        handleServerStopped(serverId, if (exitCode != 0) ServerStatus.CRASHED else ServerStatus.STOPPED)
                    }
                }

                override fun onError(error: String) {
                    scope.launch {
                        log(serverId, "[ERROR] $error")
                        handleServerStopped(serverId, ServerStatus.ERROR)
                    }
                }
            }

            val launchResult = engine.start(serverDir, serverConfig, processListener)
            if (launchResult.isFailure) {
                val error = launchResult.exceptionOrNull()?.message ?: "Unknown process launch failure"
                handleServerStopped(serverId, ServerStatus.ERROR)
                return Resource.Error("Server start failed: $error")
            }

            activeProcessHandle = launchResult.getOrNull()
            Resource.Success(Unit)
        } catch (e: Exception) {
            AppLogger.e("ServerController", "Exception starting server: ${e.message}", e)
            handleServerStopped(serverId, ServerStatus.ERROR)
            Resource.Error("Failed to start server: ${e.message}", e)
        }
    }

    suspend fun stopServer(serverId: String): Resource<Unit> {
        val handle = activeProcessHandle ?: return Resource.Success(Unit)
        val engine = activeEngine

        _serverStatus.value = ServerStatus.STOPPING
        serverRepository.updateStatus(serverId, ServerStatus.STOPPING)
        log(serverId, "[MineHost] Initiating graceful server shutdown...")

        // Stop tunnel first to cleanly disconnect incoming connections
        tunnelManager.stopTunnel()
        serverRepository.updatePublicEndpoint(serverId, null)

        val stoppedCleanly = engine?.stop(handle, 15_000L) ?: true
        if (stoppedCleanly) {
            log(serverId, "[MineHost] Server process terminated gracefully.")
        } else {
            log(serverId, "[MineHost] Server process was forcibly stopped after timeout.")
        }

        handleServerStopped(serverId, ServerStatus.STOPPED)
        return Resource.Success(Unit)
    }

    fun sendCommand(command: String): Boolean {
        val handle = activeProcessHandle ?: return false
        val engine = activeEngine ?: return false
        val serverId = _activeServerId.value ?: return false

        scope.launch {
            log(serverId, "> $command")
        }
        return engine.sendCommand(handle, command)
    }

    private fun isPortInUse(port: Int, isUdp: Boolean): Boolean {
        return if (isUdp) {
            try {
                DatagramSocket(port).use { false }
            } catch (e: Exception) {
                true
            }
        } else {
            try {
                ServerSocket(port).use { false }
            } catch (e: Exception) {
                true
            }
        }
    }

    private suspend fun handleServerStopped(serverId: String, finalStatus: ServerStatus) {
        _serverStatus.value = finalStatus
        serverRepository.updateStatus(serverId, finalStatus)
        serverRepository.updatePublicEndpoint(serverId, null)
        activeProcessHandle = null
        activeEngine = null
        _activeServerId.value = null

        wakeLockManager.releaseLocks()
        thermalManager.stopListening()
        stopForegroundService()
    }

    private fun startForegroundService(serverName: String, port: Int) {
        val intent = Intent(context, ServerForegroundService::class.java).apply {
            putExtra(ServerForegroundService.EXTRA_SERVER_NAME, serverName)
            putExtra(ServerForegroundService.EXTRA_PORT, port)
        }
        ContextCompat.startForegroundService(context, intent)
    }

    private fun stopForegroundService() {
        val intent = Intent(context, ServerForegroundService::class.java)
        context.stopService(intent)
    }

    private suspend fun log(serverId: String, message: String) {
        logRepository.log(serverId, message)
    }
}
