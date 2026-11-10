package com.minehost.app.networking

import com.minehost.app.core.common.NetworkProtocol
import com.minehost.app.domain.model.PublicEndpoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TunnelManager @Inject constructor(
    val provider: TunnelProvider,
    private val monitor: ConnectionMonitor
) {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    val tunnelStatus: StateFlow<com.minehost.app.core.common.TunnelStatus> = provider.status
    val publicEndpoint: StateFlow<PublicEndpoint?> = provider.publicEndpoint
    val claimUrl: StateFlow<String?> = provider.claimUrl
    val latencyMs: StateFlow<Long?> = monitor.latencyMs
    val remoteReachable: StateFlow<Boolean?> = monitor.remoteReachable

    private var localPort: Int = 0
    private var protocol: NetworkProtocol = NetworkProtocol.UDP

    init {
        // Monitoring follows the REAL endpoint: it starts whenever the provider reports one (even
        // if that happens after start() already returned), and stops when the endpoint disappears.
        scope.launch {
            provider.publicEndpoint.collect { endpoint ->
                if (endpoint != null) monitor.startMonitoring(endpoint, localPort, protocol)
                else monitor.stopMonitoring()
            }
        }
    }

    /**
     * Each part of the path reported separately. Unknown stays unknown (null); nothing here
     * is inferred from the tunnel merely being "connected".
     */
    val health: Flow<TunnelHealth> = combine(
        provider.status,
        provider.publicEndpoint,
        provider.claimUrl,
        monitor.localServerHealthy,
        monitor.remoteReachable
    ) { status, endpoint, claim, local, remote ->
        val running = status != com.minehost.app.core.common.TunnelStatus.DISCONNECTED &&
            status != com.minehost.app.core.common.TunnelStatus.ERROR
        TunnelHealth(
            agentRunning = running,
            authenticated = when {
                claim != null -> false
                endpoint != null -> true
                else -> null
            },
            endpointAssigned = endpoint != null,
            localServerHealthy = local,
            remoteReachable = remote
        )
    }

    suspend fun startTunnel(localPort: Int, protocol: NetworkProtocol): Result<PublicEndpoint> {
        this.localPort = localPort
        this.protocol = protocol
        return provider.start(localPort, protocol)
    }

    suspend fun stopTunnel(): Result<Unit> = provider.stop()

    suspend fun reconnect(): Result<PublicEndpoint> = provider.reconnect()
}
