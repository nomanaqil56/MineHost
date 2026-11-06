package com.minehost.app.networking

import com.minehost.app.core.common.NetworkProtocol
import com.minehost.app.core.common.TunnelStatus
import com.minehost.app.domain.model.PublicEndpoint
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TunnelManager @Inject constructor(
    val provider: TunnelProvider,
    private val monitor: ConnectionMonitor
) {
    val tunnelStatus: StateFlow<TunnelStatus> = provider.status
    val publicEndpoint: StateFlow<PublicEndpoint?> = provider.publicEndpoint
    val claimUrl: StateFlow<String?> = provider.claimUrl
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
