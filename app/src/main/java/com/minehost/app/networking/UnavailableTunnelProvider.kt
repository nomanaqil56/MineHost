package com.minehost.app.networking

import com.minehost.app.core.common.NetworkProtocol
import com.minehost.app.core.common.TunnelStatus
import com.minehost.app.domain.model.PublicEndpoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Safe default until MineHost operates its own control plane and relay network.
 * This deliberately does not fall back to a managed third-party tunnel provider.
 */
@Singleton
class UnavailableTunnelProvider @Inject constructor() : TunnelProvider {
    override val id: String = "minehost-network-unavailable"
    override val name: String = "MineHost Network (not available yet)"

    private val _status = MutableStateFlow(TunnelStatus.DISCONNECTED)
    override val status: StateFlow<TunnelStatus> = _status

    private val _publicEndpoint = MutableStateFlow<PublicEndpoint?>(null)
    override val publicEndpoint: StateFlow<PublicEndpoint?> = _publicEndpoint

    private val _claimUrl = MutableStateFlow<String?>(null)
    override val claimUrl: StateFlow<String?> = _claimUrl

    override suspend fun start(localPort: Int, protocol: NetworkProtocol): Result<PublicEndpoint> {
        _status.value = TunnelStatus.DISCONNECTED
        _publicEndpoint.value = null
        _claimUrl.value = null
        return Result.failure(
            IllegalStateException(
                "MineHost public connectivity is not available in this build. " +
                    "The server can still be used by players on the same local network."
            )
        )
    }

    override suspend fun stop(): Result<Unit> {
        _status.value = TunnelStatus.DISCONNECTED
        _publicEndpoint.value = null
        _claimUrl.value = null
        return Result.success(Unit)
    }

    override suspend fun reconnect(): Result<PublicEndpoint> =
        Result.failure(
            IllegalStateException(
                "MineHost's own public network is not available yet. No third-party tunnel will be started."
            )
        )

    override fun getDiagnosticLogs(): List<String> =
        listOf("MineHost public connectivity is not implemented in this build.")
}
