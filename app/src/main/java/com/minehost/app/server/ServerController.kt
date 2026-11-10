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
import com.minehost.app.domain.repository.PlayerRepository
import com.minehost.app.domain.repository.ServerRepository
import com.minehost.app.networking.TunnelManager
import com.minehost.app.services.ServerForegroundService
import com.minehost.app.services.ThermalManager
import com.minehost.app.services.WakeLockManager
import com.minehost.app.world.WorldManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.net.DatagramSocket
import java.net.ServerSocket
import java.util.UUID
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
    private val playerRepository: PlayerRepository,
    private val wakeLockManager: WakeLockManager,
    private val thermalManager: ThermalManager
) {
    private companion object {
        /** Fixed wait after `save-off` + flush before a backup reads the world files. */
        const val SAVE_SETTLE_MS = 5_000L
    }

    private val scope = CoroutineScope(Dispatchers.IO)
    /** Serializes start/stop so a stop request cannot race process-handle creation. */
    private val lifecycleMutex = Mutex()

    private val _activeServerId = MutableStateFlow<String?>(null)
    val activeServerId: StateFlow<String?> = _activeServerId.asStateFlow()

    private val _serverStatus = MutableStateFlow(ServerStatus.STOPPED)
    val serverStatus: StateFlow<ServerStatus> = _serverStatus.asStateFlow()

    private val playerTracker = PlayerTracker()
    private val _onlinePlayers = MutableStateFlow<Set<String>>(emptySet())
    /** Players seen joining in the console of the running server (best effort; see [PlayerTracker]). */
    val onlinePlayers: StateFlow<Set<String>> = _onlinePlayers.asStateFlow()

    private var activeProcessHandle: ServerProcessHandle? = null
    private var activeEngine: ServerEngine? = null
    /** Distinguishes process generations, including restarts of the same server ID. */
    private var activeSessionToken: String? = null

    suspend fun startServer(serverId: String): Resource<Unit> = lifecycleMutex.withLock {
        val server = serverRepository.getServerById(serverId)
            ?: return@withLock Resource.Error("Server with ID $serverId not found")

        if (_serverStatus.value.isRunning || activeProcessHandle?.isAlive == true) {
            return@withLock Resource.Error("Another server instance is already running")
        }

        val sessionToken = UUID.randomUUID().toString()
        try {
            _activeServerId.value = serverId
            activeSessionToken = sessionToken
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
                handleServerStopped(serverId, ServerStatus.ERROR, sessionToken)
                return@withLock Resource.Error(unsupportedMsg)
            }
            activeEngine = engine

            if (!engine.isInstalled()) {
                val notInstalledMsg = "Server engine '${engine.name}' is not installed on this device. Please download and install it via Diagnostics or Setup."
                log(serverId, "[ERROR] $notInstalledMsg")
                handleServerStopped(serverId, ServerStatus.ERROR, sessionToken)
                return@withLock Resource.Error(notInstalledMsg)
            }

            if (server.edition == MinecraftEdition.JAVA && !server.eulaAccepted) {
                val eulaMsg = "You must accept the Minecraft EULA (https://aka.ms/MinecraftEULA) before starting a Java server."
                log(serverId, "[ERROR] $eulaMsg")
                handleServerStopped(serverId, ServerStatus.ERROR, sessionToken)
                return@withLock Resource.Error(eulaMsg)
            }

            // 2. Validate port conflict before starting
            val isUdp = server.edition == MinecraftEdition.BEDROCK
            if (isPortInUse(server.localPort, isUdp)) {
                val portConflictMsg = "Port ${server.localPort} (${if (isUdp) "UDP" else "TCP"}) is already bound by another service on this device. Please select a different port in server settings."
                log(serverId, "[ERROR] $portConflictMsg")
                handleServerStopped(serverId, ServerStatus.ERROR, sessionToken)
                return@withLock Resource.Error(portConflictMsg)
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
                maxMemoryMb = server.maxMemoryMb,
                eulaAccepted = server.eulaAccepted
            )

            log(serverId, "[MineHost] Writing engine configuration files...")
            val configureResult = engine.configure(serverDir, serverConfig)
            if (configureResult.isFailure) {
                val configError = configureResult.exceptionOrNull()?.message ?: "unknown error"
                val msg = "Could not write server configuration files: $configError"
                log(serverId, "[ERROR] $msg")
                handleServerStopped(serverId, ServerStatus.ERROR, sessionToken)
                return@withLock Resource.Error(msg)
            }

            // 5. Start foreground service
            startForegroundService(server.name, server.localPort)

            // 6. Launch real engine process
            val processListener = object : ProcessListener {
                override fun onLog(line: String) {
                    if (playerTracker.onLine(line)) {
                        val before = _onlinePlayers.value
                        val after = playerTracker.names()
                        _onlinePlayers.value = after
                        // Remember players who just joined so the Players screen lists real players.
                        (after - before).forEach { name ->
                            scope.launch { playerRepository.recordJoin(serverId, name, System.currentTimeMillis()) }
                        }
                    }
                    scope.launch { log(serverId, line) }
                }

                override fun onStarted() {
                    scope.launch {
                        // A late readiness callback must not resurrect a server that is stopping
                        // or overwrite the state of a different server instance.
                        if (_activeServerId.value != serverId || activeSessionToken != sessionToken || _serverStatus.value != ServerStatus.STARTING) {
                            AppLogger.w("ServerController", "Ignoring stale onStarted callback for $serverId")
                            return@launch
                        }
                        _serverStatus.value = ServerStatus.ONLINE
                        serverRepository.updateStatus(serverId, ServerStatus.ONLINE)
                        log(serverId, "[MineHost] Server process confirmed ONLINE on port ${server.localPort}!")

                        // 7. Connect tunnel if enabled. Re-check state after the suspend call:
                        // stopServer may have run while the tunnel provider was connecting.
                        if (server.autoStartTunnel &&
                            _activeServerId.value == serverId &&
                            _serverStatus.value == ServerStatus.ONLINE
                        ) {
                            log(serverId, "[MineHost] Initiating worldwide reverse tunnel...")
                            val tunnelResult = tunnelManager.startTunnel(server.localPort, server.edition.defaultProtocol)
                            tunnelResult.onSuccess { endpoint ->
                                if (_activeServerId.value == serverId && activeSessionToken == sessionToken && _serverStatus.value == ServerStatus.ONLINE) {
                                    serverRepository.updatePublicEndpoint(serverId, endpoint)
                                    log(serverId, "[MineHost] Tunnel online! Public endpoint: ${endpoint.fullAddress}")
                                } else {
                                    // The server stopped while the tunnel was being established.
                                    tunnelManager.stopTunnel()
                                }
                            }.onFailure { err ->
                                log(serverId, "[MineHost] Tunnel notice: ${err.message}. Server is running in LOCAL mode.")
                            }
                        }
                    }
                }

                override fun onStopped(exitCode: Int) {
                    scope.launch {
                        AppLogger.i("ServerController", "Server process exited with code: $exitCode")
                        handleServerStopped(serverId, if (exitCode != 0) ServerStatus.CRASHED else ServerStatus.STOPPED, sessionToken)
                    }
                }

                override fun onError(error: String) {
                    scope.launch {
                        log(serverId, "[ERROR] $error")
                        handleServerStopped(serverId, ServerStatus.ERROR, sessionToken)
                    }
                }
            }

            val launchResult = engine.start(serverDir, serverConfig, processListener)
            if (launchResult.isFailure) {
                val error = launchResult.exceptionOrNull()?.message ?: "Unknown process launch failure"
                handleServerStopped(serverId, ServerStatus.ERROR, sessionToken)
                return@withLock Resource.Error("Server start failed: $error")
            }

            val processHandle = launchResult.getOrNull()
            if (processHandle == null) {
                handleServerStopped(serverId, ServerStatus.ERROR, sessionToken)
                return@withLock Resource.Error("Server engine returned success without a process handle.")
            }
            activeProcessHandle = processHandle
            if (!processHandle.isAlive) {
                val finalStatus = if (_serverStatus.value == ServerStatus.ERROR) {
                    ServerStatus.ERROR
                } else {
                    ServerStatus.CRASHED
                }
                activeProcessHandle = null
                activeEngine = null
                handleServerStopped(serverId, finalStatus, sessionToken)
                return@withLock Resource.Error("Server process exited immediately after launch. Check the server console and runtime.")
            }
            Resource.Success(Unit)
        } catch (e: Exception) {
            AppLogger.e("ServerController", "Exception starting server: ${e.message}", e)
            handleServerStopped(serverId, ServerStatus.ERROR, sessionToken)
            Resource.Error("Failed to start server: ${e.message}", e)
        }
    }

    suspend fun stopServer(serverId: String): Resource<Unit> = lifecycleMutex.withLock {
        val activeId = _activeServerId.value
        if (activeId != null && activeId != serverId) {
            return@withLock Resource.Error("Server '$activeId' is currently active; cannot stop '$serverId'.")
        }
        val sessionToken = activeSessionToken
        val handle = activeProcessHandle
        if (handle == null) {
            if (activeId == serverId && _serverStatus.value.isRunning) {
                // Do not claim success when the state machine says a process is active but
                // no process handle exists. Reconcile the stale state instead.
                handleServerStopped(serverId, ServerStatus.CRASHED, sessionToken)
                return@withLock Resource.Error("The server was marked active but its process handle is missing. State was recovered; start it again.")
            }
            return@withLock Resource.Success(Unit)
        }
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

        handleServerStopped(serverId, ServerStatus.STOPPED, sessionToken)
        return@withLock Resource.Success(Unit)
    }

    fun sendCommand(command: String): Boolean {
        val handle = activeProcessHandle ?: return false
        val engine = activeEngine ?: return false
        val serverId = _activeServerId.value ?: return false

        if (!CommandSanitizer.isSafe(command)) {
            AppLogger.w("ServerController", "Rejected a console command that was blank, too long or contained control characters")
            return false
        }

        scope.launch {
            log(serverId, "> $command")
        }
        return engine.sendCommand(handle, command)
    }

    /**
     * Runs [block] (normally a world backup) while the server's world saving is paused, so the
     * files on disk are consistent. If the server is not online, [block] simply runs.
     *
     * Sequence: `save-off`, flush (`save-all flush` on Java, `save-all` on Bedrock), wait, [block],
     * then `save-on` in a `finally` so saving is always re-enabled.
     *
     * LIMITATION: there is no completion signal from the server here, so the flush wait is a fixed
     * [SAVE_SETTLE_MS]. A very large world may need longer. Not verified against a real server.
     */
    suspend fun <T> withWorldSavePaused(serverId: String, block: suspend () -> T): T {
        val online = _activeServerId.value == serverId && _serverStatus.value == ServerStatus.ONLINE
        if (!online) return block()

        val edition = serverRepository.getServerById(serverId)?.edition
        val flushCommand = if (edition == MinecraftEdition.JAVA) "save-all flush" else "save-all"

        if (!sendCommand("save-off")) {
            log(serverId, "[MineHost] WARNING: could not pause world saving; the backup may capture a world that is mid-write.")
            return block()
        }
        sendCommand(flushCommand)
        log(serverId, "[MineHost] World saving paused for backup; waiting ${SAVE_SETTLE_MS / 1000}s for the flush...")
        delay(SAVE_SETTLE_MS)
        try {
            return block()
        } finally {
            sendCommand("save-on")
            log(serverId, "[MineHost] World saving resumed.")
        }
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

    private suspend fun handleServerStopped(
        serverId: String,
        finalStatus: ServerStatus,
        sessionToken: String? = null
    ) {
        // A process-generation token prevents a late callback from a previous run (including
        // a restart of the same server ID) from clearing the new process or its wake locks.
        if (sessionToken != null && activeSessionToken != sessionToken) return
        if (_activeServerId.value != serverId) return

        serverRepository.updateStatus(serverId, finalStatus)
        serverRepository.updatePublicEndpoint(serverId, null)
        _serverStatus.value = finalStatus
        activeProcessHandle = null
        activeEngine = null
        activeSessionToken = null
        _activeServerId.value = null
        playerTracker.reset()
        _onlinePlayers.value = emptySet()

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
