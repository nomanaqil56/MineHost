package com.minehost.app.networking

import com.minehost.app.core.common.NetworkProtocol
import com.minehost.app.core.common.TunnelStatus
import com.minehost.app.domain.model.PublicEndpoint
import kotlinx.coroutines.flow.StateFlow

interface TunnelProvider {
    val id: String
    val name: String
    val status: StateFlow<TunnelStatus>
    val publicEndpoint: StateFlow<PublicEndpoint?>
    val claimUrl: StateFlow<String?>

    suspend fun start(localPort: Int, protocol: NetworkProtocol): Result<PublicEndpoint>
    suspend fun stop(): Result<Unit>
    suspend fun reconnect(): Result<PublicEndpoint>
    fun getDiagnosticLogs(): List<String>
}
