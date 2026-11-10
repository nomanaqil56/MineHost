package com.minehost.app.networking

import android.content.Context
import com.minehost.app.core.common.NetworkProtocol
import com.minehost.app.core.common.TunnelStatus
import com.minehost.app.core.logging.AppLogger
import com.minehost.app.domain.model.PublicEndpoint
import com.minehost.app.server.RuntimeManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.FileNotFoundException
import java.io.InputStreamReader
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Runs the official Playit agent and reports ONLY what the agent actually told us.
 *
 * - Status is CONNECTED only after the agent printed a public endpoint. No endpoint is ever invented.
 * - AUTHENTICATING means the agent is waiting for the user to claim it at the claim URL.
 * - If the agent exits without being asked to, it is restarted with backoff ([ReconnectPolicy]);
 *   after the last attempt the status is ERROR.
 * - Everything written to the diagnostic log goes through [LogRedactor].
 *
 * LIMITATIONS (not verified without a device and a Playit account): the exact format of the agent's
 * output is assumed by [PlayitOutputParser]; where the tunnel forwards to (the local port) is
 * configured in the Playit dashboard, not by this class; and on Android 10+ the agent can only be
 * executed if it is packaged in the app's native library directory.
 */
@Singleton
class PlayitTunnelProvider @Inject constructor(
    @ApplicationContext private val context: Context,
    private val runtimeManager: RuntimeManager
) : TunnelProvider {

    override val id: String = "playit-gg"
    override val name: String = "Playit.gg Anycast Tunnel"

    private val _status = MutableStateFlow(TunnelStatus.DISCONNECTED)
    override val status: StateFlow<TunnelStatus> = _status.asStateFlow()

    private val _publicEndpoint = MutableStateFlow<PublicEndpoint?>(null)
    override val publicEndpoint: StateFlow<PublicEndpoint?> = _publicEndpoint.asStateFlow()

    private val _claimUrl = MutableStateFlow<String?>(null)
    override val claimUrl: StateFlow<String?> = _claimUrl.asStateFlow()

    private val _agentRunning = MutableStateFlow(false)
    /** True while the agent process is alive. Says nothing about whether a tunnel exists. */
    val agentRunning: StateFlow<Boolean> = _agentRunning.asStateFlow()

    private val logs = ConcurrentLinkedQueue<String>()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val lifecycle = Mutex()

    @Volatile private var agentProcess: Process? = null
    @Volatile private var stopRequested = false
    private var outputJob: Job? = null
    private var supervisorJob: Job? = null

    private var activeLocalPort: Int = 19132
    private var activeProtocol: NetworkProtocol = NetworkProtocol.UDP

    // ------------------------------------------------------------------ public API

    override suspend fun start(localPort: Int, protocol: NetworkProtocol): Result<PublicEndpoint> =
        withContext(Dispatchers.IO) {
            // NetherNet requires TCP signaling and UDP WebRTC traffic. This experimental
            // provider has not been proven to provision both transports for one server.
            if (protocol == NetworkProtocol.BOTH) {
                val error = UnsupportedOperationException(
                    "Playit adapter is not validated for simultaneous TCP/UDP Bedrock NetherNet. " +
                        "Use LAN hosting until MineHost connectivity is available."
                )
                _status.value = TunnelStatus.ERROR
                log("[Playit] Refusing unverified dual-transport Bedrock tunnel")
                return@withContext Result.failure(error)
            }
            activeLocalPort = localPort
            activeProtocol = protocol
            stopRequested = false
            supervisorJob?.cancel()
            lifecycle.withLock {
                if (agentProcess?.isAlive == true) {
                    // Already running: report what we really have instead of starting a second agent.
                    val existing = _publicEndpoint.value
                    return@withLock if (existing != null) Result.success(existing)
                    else awaitEndpoint(agentProcess!!)
                } else {
                    launchAndAwait()
                }
            }
        }

    override suspend fun stop(): Result<Unit> = withContext(Dispatchers.IO) {
        // Set first so that a start/await in progress notices and bails out promptly.
        stopRequested = true
        supervisorJob?.cancel()
        lifecycle.withLock {
            _status.value = TunnelStatus.STOPPING
            log("[Playit] Stopping tunnel agent...")
            val p = agentProcess
            agentProcess = null
            outputJob?.cancel()
            if (p != null) {
                p.destroy()
                try {
                    if (!p.waitFor(3, TimeUnit.SECONDS)) p.destroyForcibly()
                } catch (e: InterruptedException) {
                    p.destroyForcibly()
                }
            }
            _agentRunning.value = false
            _publicEndpoint.value = null
            _claimUrl.value = null
            _status.value = TunnelStatus.DISCONNECTED
            log("[Playit] Tunnel agent stopped")
            Result.success(Unit)
        }
    }

    override suspend fun reconnect(): Result<PublicEndpoint> {
        _status.value = TunnelStatus.RECONNECTING
        stop()
        return start(activeLocalPort, activeProtocol)
    }

    override fun getDiagnosticLogs(): List<String> = logs.toList().takeLast(200)

    // ------------------------------------------------------------------ launching

    /** Must be called while holding [lifecycle]. */
    private suspend fun launchAndAwait(): Result<PublicEndpoint> {
        _claimUrl.value = null
        _publicEndpoint.value = null
        _status.value = TunnelStatus.STARTING

        val binary = runtimeManager.getPlayitExecutable()
        if (binary == null || !binary.exists()) {
            val msg = "Playit agent is not installed. Install it from Diagnostics before enabling remote access."
            log("[Playit] ERROR: $msg")
            _status.value = TunnelStatus.ERROR
            return Result.failure(FileNotFoundException(msg))
        }

        val workDir = File(context.filesDir, "minehost/tunnels/playit").apply { mkdirs() }
        restrictToOwner(workDir)
        log("[Playit] Launching agent (${activeProtocol.name}, local port $activeLocalPort)")

        val process = try {
            ProcessBuilder(binary.absolutePath)
                .directory(workDir)
                .redirectErrorStream(true)
                .start()
        } catch (e: Exception) {
            AppLogger.e("PlayitTunnelProvider", "Failed to launch agent: ${e.message}", e)
            log("[Playit] Could not launch the agent: ${e.message}")
            _status.value = TunnelStatus.ERROR
            return Result.failure(e)
        }

        agentProcess = process
        _agentRunning.value = true
        outputJob = scope.launch(Dispatchers.IO) {
            try {
                BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
                    while (isActive) {
                        val line = reader.readLine() ?: break
                        handleAgentLine(line)
                    }
                }
            } catch (_: Exception) {
                // Stream closed because the process ended; the exit watcher reports it.
            }
        }
        scope.launch(Dispatchers.IO) { watchForExit(process) }

        return awaitEndpoint(process)
    }

    private suspend fun awaitEndpoint(process: Process): Result<PublicEndpoint> {
        var waitedMs = 0L
        while (waitedMs < ENDPOINT_TIMEOUT_MS && process.isAlive && !stopRequested) {
            _publicEndpoint.value?.let { return Result.success(it) }
            _claimUrl.value?.let { claim ->
                return Result.failure(
                    IllegalStateException(
                        "Playit needs you to claim this agent before it can route traffic. Open: $claim"
                    )
                )
            }
            delay(POLL_MS)
            waitedMs += POLL_MS
        }
        _publicEndpoint.value?.let { return Result.success(it) }
        if (stopRequested) return Result.failure(IllegalStateException("Tunnel start was cancelled."))
        if (!process.isAlive) {
            _status.value = TunnelStatus.ERROR
            return Result.failure(IllegalStateException("The Playit agent exited during startup. See the tunnel log in Diagnostics."))
        }
        // Still alive but no endpoint yet: leave it running (it may be waiting on the network) but do not claim success.
        return Result.failure(
            TimeoutException("Playit did not report a public endpoint within ${ENDPOINT_TIMEOUT_MS / 1000}s. Check the network connection and claim status.")
        )
    }

    // ------------------------------------------------------------------ supervision

    private fun watchForExit(process: Process) {
        val exitCode = try {
            process.waitFor()
        } catch (e: InterruptedException) {
            return
        }
        if (process !== agentProcess) return // an older process, already replaced or stopped
        log("[Playit] Agent process exited with code $exitCode")
        _agentRunning.value = false
        _publicEndpoint.value = null
        if (stopRequested) return
        if (supervisorJob?.isActive != true) superviseRestart()
    }

    private fun superviseRestart() {
        supervisorJob = scope.launch {
            for (attempt in 1..ReconnectPolicy.MAX_ATTEMPTS) {
                if (stopRequested) return@launch
                val waitMs = ReconnectPolicy.delayMs(attempt)
                _status.value = TunnelStatus.RECONNECTING
                log("[Playit] Agent stopped unexpectedly; restart attempt $attempt/${ReconnectPolicy.MAX_ATTEMPTS} in ${waitMs / 1000}s")
                delay(waitMs)
                if (stopRequested) return@launch

                val result = lifecycle.withLock {
                    if (stopRequested) null else launchAndAwait()
                } ?: return@launch

                if (result.isSuccess) {
                    log("[Playit] Reconnected")
                    return@launch
                }
                // Needs the user (claim) or the agent keeps running without an endpoint: stop retrying here.
                if (_claimUrl.value != null || agentProcess?.isAlive == true) return@launch
            }
            if (!stopRequested) {
                _status.value = TunnelStatus.ERROR
                log("[Playit] Giving up after ${ReconnectPolicy.MAX_ATTEMPTS} restart attempts")
            }
        }
    }

    // ------------------------------------------------------------------ output handling

    private fun handleAgentLine(line: String) {
        log("[Playit] $line")
        when (val event = PlayitOutputParser.parse(line)) {
            is PlayitOutputParser.Event.ClaimRequired -> {
                _claimUrl.value = event.url
                _status.value = TunnelStatus.AUTHENTICATING
            }
            is PlayitOutputParser.Event.EndpointAssigned -> {
                _claimUrl.value = null
                _publicEndpoint.value = PublicEndpoint(event.host, event.port, activeProtocol.name)
                _status.value = TunnelStatus.CONNECTED
            }
            null -> Unit
        }
    }

    // ------------------------------------------------------------------ helpers

    /** The agent keeps its own credentials in this directory; make it private to the app's user. */
    private fun restrictToOwner(dir: File) {
        dir.setReadable(false, false); dir.setReadable(true, true)
        dir.setWritable(false, false); dir.setWritable(true, true)
        dir.setExecutable(false, false); dir.setExecutable(true, true)
    }

    private fun log(message: String) {
        val safe = LogRedactor.redact(message)
        logs.add("[${System.currentTimeMillis()}] $safe")
        while (logs.size > MAX_LOG_LINES) logs.poll()
        AppLogger.d("PlayitTunnelProvider", safe)
    }

    private companion object {
        const val ENDPOINT_TIMEOUT_MS = 20_000L
        const val POLL_MS = 500L
        const val MAX_LOG_LINES = 500
    }
}
