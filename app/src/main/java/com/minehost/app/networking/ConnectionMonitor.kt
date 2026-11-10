package com.minehost.app.networking

import com.minehost.app.core.common.NetworkProtocol
import com.minehost.app.core.logging.AppLogger
import com.minehost.app.domain.model.PublicEndpoint
import com.minehost.app.server.ReadinessProbe
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
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "ConnectionMonitor"

/**
 * Checks the tunnel the way a Minecraft client would: with a real Minecraft protocol request sent to
 * the PUBLIC endpoint (RakNet ping for Bedrock/UDP, Server List Ping for Java/TCP). A plain TCP
 * connect, which this class used to use, cannot tell whether a UDP game server is reachable.
 *
 * It also probes the server on localhost, so "the server is down" and "the tunnel is down" are
 * reported separately.
 */
@Singleton
class ConnectionMonitor @Inject constructor() {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var monitorJob: Job? = null

    private val _latencyMs = MutableStateFlow<Long?>(null)
    /** Round-trip time through the public endpoint; null when not reachable or not checked yet. */
    val latencyMs: StateFlow<Long?> = _latencyMs.asStateFlow()

    private val _remoteReachable = MutableStateFlow<Boolean?>(null)
    /** True/false once probed through the public endpoint; null = not checked yet. */
    val remoteReachable: StateFlow<Boolean?> = _remoteReachable.asStateFlow()

    private val _localServerHealthy = MutableStateFlow<Boolean?>(null)
    /** Whether the Minecraft server answers on localhost; null = not checked yet. */
    val localServerHealthy: StateFlow<Boolean?> = _localServerHealthy.asStateFlow()

    fun startMonitoring(endpoint: PublicEndpoint, localPort: Int, protocol: NetworkProtocol) {
        stopMonitoring()
        val tracker = ReachabilityTracker()
        monitorJob = scope.launch {
            delay(INITIAL_DELAY_MS)
            while (isActive) {
                val local = probe("127.0.0.1", localPort, protocol)
                _localServerHealthy.value = local != null

                val remote = probe(endpoint.hostname, endpoint.port, protocol)
                _remoteReachable.value = tracker.record(remote != null)
                _latencyMs.value = remote
                AppLogger.d(TAG, "local=${local}ms remote=${remote}ms reachable=${tracker.reachable}")
                delay(INTERVAL_MS)
            }
        }
    }

    fun stopMonitoring() {
        monitorJob?.cancel()
        monitorJob = null
        _latencyMs.value = null
        _remoteReachable.value = null
        _localServerHealthy.value = null
    }

    private fun probe(host: String, port: Int, protocol: NetworkProtocol): Long? = when (protocol) {
        NetworkProtocol.UDP -> ReadinessProbe.measureBedrockMs(host, port)
        NetworkProtocol.TCP -> ReadinessProbe.measureJavaMs(host, port)
        NetworkProtocol.BOTH -> ReadinessProbe.measureBedrockMs(host, port) ?: ReadinessProbe.measureJavaMs(host, port)
    }

    private companion object {
        const val INITIAL_DELAY_MS = 3_000L
        const val INTERVAL_MS = 10_000L
    }
}
