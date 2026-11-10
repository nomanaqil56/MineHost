package com.minehost.app.features.servers

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.minehost.app.core.common.ServerStatus
import com.minehost.app.core.common.TunnelStatus
import com.minehost.app.domain.model.PublicEndpoint
import com.minehost.app.domain.model.ServerModel
import com.minehost.app.domain.repository.ServerRepository
import com.minehost.app.networking.TunnelManager
import com.minehost.app.server.ServerController
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ServerDetailUiState(
    val server: ServerModel? = null,
    val isRunning: Boolean = false,
    val currentStatus: ServerStatus = ServerStatus.STOPPED,
    val publicEndpoint: PublicEndpoint? = null,
    val tunnelStatus: TunnelStatus = TunnelStatus.DISCONNECTED,
    val claimUrl: String? = null,
    val latencyMs: Long? = null,
    /** True/false once probed through the public endpoint; null = not checked (never assumed). */
    val remoteReachable: Boolean? = null,
    /** This device's LAN IPv4 address (same-Wi-Fi hosting), or null if not on a local network. */
    val localAddress: String? = null,
    /** Players seen joining via the console; null when the server is not online. */
    val onlinePlayers: Int? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val showEulaDialog: Boolean = false
)

private data class ServerDetailTransient(
    val error: String? = null,
    val showEula: Boolean = false
)

private data class TunnelUiInfo(
    val endpoint: PublicEndpoint?,
    val status: TunnelStatus,
    val claimUrl: String?,
    val latencyMs: Long?,
    val remoteReachable: Boolean?
)

@HiltViewModel
class ServerDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val serverRepository: ServerRepository,
    private val serverController: ServerController,
    private val tunnelManager: TunnelManager,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    val serverId: String = checkNotNull(savedStateHandle["serverId"])

    private val _transient = kotlinx.coroutines.flow.MutableStateFlow(ServerDetailTransient())

    /** Re-reads the LAN address every few seconds so a Wi-Fi change shows up. */
    private val localAddressFlow: kotlinx.coroutines.flow.Flow<String?> =
        kotlinx.coroutines.flow.flow {
            while (true) {
                emit(com.minehost.app.networking.LocalNetwork.detect(appContext).firstOrNull())
                kotlinx.coroutines.delay(5_000L)
            }
        }.flowOn(kotlinx.coroutines.Dispatchers.IO)

    val uiState: StateFlow<ServerDetailUiState> = combine(
        combine(
            serverRepository.getServerFlowById(serverId),
            serverController.activeServerId,
            serverController.serverStatus
        ) { server, activeId, status ->
            Triple(server, activeId, status)
        },
        combine(
            tunnelManager.publicEndpoint,
            tunnelManager.tunnelStatus,
            tunnelManager.claimUrl,
            tunnelManager.latencyMs,
            tunnelManager.remoteReachable
        ) { endpoint, tunnelStat, claim, ping, reachable ->
            TunnelUiInfo(endpoint, tunnelStat, claim, ping, reachable)
        },
        serverController.onlinePlayers,
        localAddressFlow
    ) { (server, activeId, status), tunnelInfo, players, lanAddress ->
        val isThisActive = activeId == serverId
        val effectiveStatus = if (isThisActive) status else (server?.status ?: ServerStatus.STOPPED)
        val effectiveEndpoint = if (isThisActive) tunnelInfo.endpoint ?: server?.publicEndpoint else server?.publicEndpoint

        ServerDetailUiState(
            server = server,
            isRunning = isThisActive && status.isRunning,
            currentStatus = effectiveStatus,
            publicEndpoint = effectiveEndpoint,
            tunnelStatus = if (isThisActive) tunnelInfo.status else TunnelStatus.DISCONNECTED,
            claimUrl = if (isThisActive) tunnelInfo.claimUrl else null,
            latencyMs = if (isThisActive) tunnelInfo.latencyMs else null,
            remoteReachable = if (isThisActive) tunnelInfo.remoteReachable else null,
            onlinePlayers = if (isThisActive && status == ServerStatus.ONLINE) players.size else null,
            localAddress = lanAddress,
            isLoading = false
        )
    }.combine(_transient) { ui, transient ->
        ui.copy(errorMessage = transient.error, showEulaDialog = transient.showEula)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ServerDetailUiState(isLoading = true)
    )

    fun toggleServer() {
        viewModelScope.launch {
            val isCurrentRunning = serverController.activeServerId.value == serverId &&
                    serverController.serverStatus.value.isRunning
            if (isCurrentRunning) {
                val stopResult = serverController.stopServer(serverId)
                if (stopResult is com.minehost.app.core.common.Resource.Error) {
                    _transient.value = _transient.value.copy(error = stopResult.message)
                }
            } else {
                val server = serverRepository.getServerById(serverId)
                if (server != null &&
                    server.edition == com.minehost.app.core.common.MinecraftEdition.JAVA &&
                    !server.eulaAccepted
                ) {
                    // Never accept the EULA silently: ask the user first.
                    _transient.value = ServerDetailTransient(showEula = true)
                    return@launch
                }
                startAndReportErrors()
            }
        }
    }

    private suspend fun startAndReportErrors() {
        _transient.value = ServerDetailTransient()
        val result = serverController.startServer(serverId)
        if (result is com.minehost.app.core.common.Resource.Error) {
            _transient.value = ServerDetailTransient(error = result.message)
        }
    }

    fun acceptEulaAndStart() {
        viewModelScope.launch {
            serverRepository.updateEulaConsent(serverId, true)
            startAndReportErrors()
        }
    }

    fun declineEula() {
        _transient.value = ServerDetailTransient()
    }

    fun dismissError() {
        _transient.value = _transient.value.copy(error = null)
    }

    fun deleteServer(onDeleted: () -> Unit) {
        viewModelScope.launch {
            serverController.stopServer(serverId)
            serverRepository.deleteServer(serverId)
            onDeleted()
        }
    }
}
