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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.FileNotFoundException
import java.io.InputStreamReader
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.TimeoutException
import javax.inject.Inject
import javax.inject.Singleton

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

    private val logs = ConcurrentLinkedQueue<String>()
    private var agentProcess: Process? = null
    private var monitorJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    private var activeLocalPort: Int = 19132
    private var activeProtocol: NetworkProtocol = NetworkProtocol.UDP

    // Regex patterns for real Playit stdout parsing
    private val claimUrlPattern = Regex("https://playit\\.gg/claim/[a-zA-Z0-9]+")
    private val endpointPattern = Regex("([a-zA-Z0-9\\-]+\\.(?:gl\\.joinmc\\.link|joinmc\\.link|ply\\.gg)):(\\d+)")

    override suspend fun start(localPort: Int, protocol: NetworkProtocol): Result<PublicEndpoint> =
        withContext(Dispatchers.IO) {
            activeLocalPort = localPort
            activeProtocol = protocol
            _status.value = TunnelStatus.CONNECTING
            _claimUrl.value = null
            _publicEndpoint.value = null

            val playitBinary = runtimeManager.getPlayitExecutable()
            if (playitBinary == null || !playitBinary.exists()) {
                val errorMsg = "Playit agent binary was not found. Please install the Playit agent from Diagnostics or Runtime Setup."
                log("[Playit] ERROR: $errorMsg")
                _status.value = TunnelStatus.ERROR
                return@withContext Result.failure(FileNotFoundException(errorMsg))
            }

            val workDir = File(context.filesDir, "minehost/tunnels/playit").apply { mkdirs() }
            log("[Playit] Launching agent binary: ${playitBinary.absolutePath} for $protocol on port $localPort...")

            return@withContext try {
                val pb = ProcessBuilder(playitBinary.absolutePath)
                    .directory(workDir)
                    .redirectErrorStream(true)

                val p = pb.start()
                agentProcess = p

                monitorJob = scope.launch(Dispatchers.IO) {
                    BufferedReader(InputStreamReader(p.inputStream)).use { reader ->
                        while (isActive) {
                            val line = reader.readLine() ?: break
                            parseAgentOutput(line)
                        }
                    }
                }

                scope.launch(Dispatchers.IO) {
                    val exitCode = p.waitFor()
                    log("[Playit] Agent process exited with code $exitCode")
                    _status.value = TunnelStatus.DISCONNECTED
                    _publicEndpoint.value = null
                }

                // Wait up to 15 seconds for genuine endpoint assignment or claim URL
                var waitedMs = 0L
                val pollInterval = 500L
                val timeoutMs = 15_000L

                while (waitedMs < timeoutMs && p.isAlive) {
                    val endpoint = _publicEndpoint.value
                    if (endpoint != null) {
                        return@withContext Result.success(endpoint)
                    }

                    val claim = _claimUrl.value
                    if (claim != null) {
                        return@withContext Result.failure(
                            IllegalStateException("Playit agent requires account claim before routing traffic. Claim URL: $claim")
                        )
                    }

                    delay(pollInterval)
                    waitedMs += pollInterval
                }

                if (!p.isAlive) {
                    _status.value = TunnelStatus.ERROR
                    return@withContext Result.failure(IllegalStateException("Playit agent terminated unexpectedly during startup"))
                }

                // If still connecting without endpoint, report waiting timeout
                _status.value = TunnelStatus.CONNECTING
                Result.failure(TimeoutException("Waiting for Playit Anycast tunnel endpoint assignment timed out. Check network connection or claim status."))
            } catch (e: Exception) {
                AppLogger.e("PlayitTunnelProvider", "Failed to start tunnel: ${e.message}", e)
                _status.value = TunnelStatus.ERROR
                log("[Playit] Error: ${e.message}")
                Result.failure(e)
            }
        }

    private fun parseAgentOutput(line: String) {
        log("[Playit] $line")

        // 1. Detect Claim URL
        val claimMatch = claimUrlPattern.find(line)
        if (claimMatch != null) {
            val url = claimMatch.value
            _claimUrl.value = url
            log("[Playit] Action Required: Claim agent at $url")
            return
        }

        // 2. Detect Public Endpoint
        val endpointMatch = endpointPattern.find(line)
        if (endpointMatch != null) {
            val host = endpointMatch.groupValues[1]
            val port = endpointMatch.groupValues[2].toIntOrNull() ?: activeLocalPort
            val endpoint = PublicEndpoint(host, port, activeProtocol.name)

            _publicEndpoint.value = endpoint
            _status.value = TunnelStatus.CONNECTED
            log("[Playit] Genuine public endpoint confirmed: ${endpoint.fullAddress}")
        }
    }

    override suspend fun stop(): Result<Unit> = withContext(Dispatchers.IO) {
        log("[Playit] Stopping tunnel agent...")
        monitorJob?.cancel()
        agentProcess?.destroy()
        agentProcess = null
        _status.value = TunnelStatus.DISCONNECTED
        _publicEndpoint.value = null
        _claimUrl.value = null
        log("[Playit] Tunnel agent stopped")
        Result.success(Unit)
    }

    override suspend fun reconnect(): Result<PublicEndpoint> {
        _status.value = TunnelStatus.RECONNECTING
        stop()
        return start(activeLocalPort, activeProtocol)
    }

    override fun getDiagnosticLogs(): List<String> = logs.toList().takeLast(200)

    private fun log(message: String) {
        logs.add("[${System.currentTimeMillis()}] $message")
        AppLogger.d("PlayitTunnelProvider", message)
    }
}
