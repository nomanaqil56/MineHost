package com.minehost.app.features.servers

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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ServerDetailUiState(
    val server: ServerModel? = null,
    val isRunning: Boolean = false,
    val currentStatus: ServerStatus = ServerStatus.STOPPED,
    val publicEndpoint: PublicEndpoint? = null,
    val tunnelStatus: TunnelStatus = TunnelStatus.DISCONNECTED,
    val latencyMs: Long? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class ServerDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val serverRepository: ServerRepository,
    private val serverController: ServerController,
    private val tunnelManager: TunnelManager
) : ViewModel() {

    val serverId: String = checkNotNull(savedStateHandle["serverId"])

    val uiState: StateFlow<ServerDetailUiState> = combine(
        serverRepository.getServerFlowById(serverId),
        serverController.activeServerId,
        serverController.serverStatus,
        tunnelManager.publicEndpoint,
        tunnelManager.tunnelStatus,
        tunnelManager.latencyMs
    ) { server, activeId, status, endpoint, tunnelStat, ping ->
        val isThisActive = activeId == serverId
        val effectiveStatus = if (isThisActive) status else (server?.status ?: ServerStatus.STOPPED)
        val effectiveEndpoint = if (isThisActive) endpoint ?: server?.publicEndpoint else server?.publicEndpoint

        ServerDetailUiState(
            server = server,
            isRunning = isThisActive && status.isRunning,
            currentStatus = effectiveStatus,
            publicEndpoint = effectiveEndpoint,
            tunnelStatus = if (isThisActive) tunnelStat else TunnelStatus.DISCONNECTED,
            latencyMs = if (isThisActive) ping else null,
            isLoading = false
        )
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
                serverController.stopServer(serverId)
            } else {
                serverController.startServer(serverId)
            }
        }
    }

    fun deleteServer(onDeleted: () -> Unit) {
        viewModelScope.launch {
            serverController.stopServer(serverId)
            serverRepository.deleteServer(serverId)
            onDeleted()
        }
    }
}

