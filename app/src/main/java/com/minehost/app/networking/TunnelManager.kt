package com.minehost.app.networking

import com.minehost.app.core.common.NetworkProtocol
import com.minehost.app.core.common.TunnelStatus
import com.minehost.app.core.logging.AppLogger
import com.minehost.app.domain.model.PublicEndpoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.net.InetSocketAddress
import java.net.Socket
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConnectionMonitor @Inject constructor(
    private val tunnelProvider: TunnelProvider
) {
    private val _latencyMs = MutableStateFlow<Long?>(null)
    val latencyMs: StateFlow<Long?> = _latencyMs.asStateFlow()

    private var monitorJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    fun startMonitoring(endpoint: PublicEndpoint) {
        monitorJob?.cancel()
        monitorJob = scope.launch {
            while (isActive) {
                measureLatency(endpoint)
                delay(30_000L) // Check every 30 seconds
            }
        }
    }

    fun stopMonitoring() {
        monitorJob?.cancel()
        _latencyMs.value = null
    }

    private fun measureLatency(endpoint: PublicEndpoint) {
        try {
            val startTime = System.currentTimeMillis()
            Socket().use { socket ->
                socket.connect(InetSocketAddress(endpoint.hostname, endpoint.port), 5000)
                val duration = System.currentTimeMillis() - startTime
                _latencyMs.value = duration
                AppLogger.d("ConnectionMonitor", "Measured latency to ${endpoint.fullAddress}: ${duration}ms")
            }
        } catch (e: Exception) {
            // In UDP mode TCP socket connection will not handshake, so simulate ping or ignore failure
            _latencyMs.value = null
        }
    }
}

@Singleton
class TunnelManager @Inject constructor(
    val provider: TunnelProvider,
    private val monitor: ConnectionMonitor
) {
    val tunnelStatus: StateFlow<TunnelStatus> = provider.status
    val publicEndpoint: StateFlow<PublicEndpoint?> = provider.publicEndpoint
    val latencyMs: StateFlow<Long?> = monitor.latencyMs

    suspend fun startTunnel(localPort: Int, protocol: NetworkProtocol): Result<PublicEndpoint> {
        val result = provider.start(localPort, protocol)
        result.getOrNull()?.let { endpoint ->
            monitor.startMonitoring(endpoint)
        }
        return result
    }

    suspend fun stopTunnel(): Result<Unit> {
        monitor.stopMonitoring()
        return provider.stop()
    }

    suspend fun reconnect(): Result<PublicEndpoint> {
        val result = provider.reconnect()
        result.getOrNull()?.let { endpoint ->
            monitor.startMonitoring(endpoint)
        }
        return result
    }
}
