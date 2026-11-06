package com.minehost.app.features.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.minehost.app.core.common.ServerStatus
import com.minehost.app.domain.model.ServerModel
import com.minehost.app.domain.repository.ServerRepository
import com.minehost.app.server.ServerController
import com.minehost.app.world.WorldManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardUiState(
    val servers: List<ServerModel> = emptyList(),
    val availableStorageBytes: Long = 0L,
    val activeServerId: String? = null,
    val globalStatus: ServerStatus = ServerStatus.STOPPED,
    val isLoading: Boolean = false
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val serverRepository: ServerRepository,
    private val serverController: ServerController,
    private val worldManager: WorldManager
) : ViewModel() {

    val uiState: StateFlow<DashboardUiState> = combine(
        serverRepository.getAllServers(),
        serverController.activeServerId,
        serverController.serverStatus
    ) { servers, activeId, status ->
        DashboardUiState(
            servers = servers,
            availableStorageBytes = worldManager.getAvailableStorageBytes(),
            activeServerId = activeId,
            globalStatus = status,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState(isLoading = true)
    )

    fun toggleServer(serverId: String) {
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
}
