package com.minehost.app.features.worlds

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.minehost.app.core.common.Resource
import com.minehost.app.domain.model.BackupModel
import com.minehost.app.domain.repository.BackupRepository
import com.minehost.app.domain.repository.ServerRepository
import com.minehost.app.server.ServerController
import com.minehost.app.world.WorldBackupManager
import com.minehost.app.world.WorldManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BackupsUiState(
    val backups: List<BackupModel> = emptyList(),
    val worldSizeBytes: Long = 0L,
    val isCreatingBackup: Boolean = false,
    val isRestoring: Boolean = false,
    val progress: Float? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class BackupsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val serverRepository: ServerRepository,
    private val backupRepository: BackupRepository,
    private val worldBackupManager: WorldBackupManager,
    private val worldManager: WorldManager,
    private val serverController: ServerController
) : ViewModel() {

    val serverId: String = checkNotNull(savedStateHandle["serverId"])

    val backups: StateFlow<List<BackupModel>> = backupRepository.getBackupsForServer(serverId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _uiState = MutableStateFlow(BackupsUiState())
    val uiState: StateFlow<BackupsUiState> = _uiState.asStateFlow()

    init {
        loadWorldSize()
    }

    private fun loadWorldSize() {
        val size = worldManager.calculateWorldSize(serverId)
        _uiState.value = _uiState.value.copy(worldSizeBytes = size)
    }

    fun createBackup(note: String? = null) {
        viewModelScope.launch {
            val server = serverRepository.getServerById(serverId) ?: return@launch
            _uiState.value = _uiState.value.copy(isCreatingBackup = true, errorMessage = null)

            // If the server is online, world saving is paused while the archive is written so the
            // backup is not taken mid-write (see ServerController.withWorldSavePaused).
            val result = try {
                serverController.withWorldSavePaused(serverId) {
                    worldBackupManager.createBackup(
                        serverId = serverId,
                        serverName = server.name,
                        worldName = "world",
                        note = note
                    ) { progress ->
                        _uiState.value = _uiState.value.copy(progress = progress)
                    }
                }
            } catch (e: Exception) {
                Resource.Error("Could not safely pause world saving for backup: ${e.message}", e)
            }

            when (result) {
                is Resource.Success -> {
                    _uiState.value = _uiState.value.copy(isCreatingBackup = false, progress = null)
                }
                is Resource.Error -> {
                    _uiState.value = _uiState.value.copy(isCreatingBackup = false, progress = null, errorMessage = result.message)
                }
                else -> Unit
            }
        }
    }

    fun restoreBackup(backupId: String) {
        viewModelScope.launch {
            if (serverController.activeServerId.value == serverId && serverController.serverStatus.value.isRunning) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Stop the server before restoring a backup. Restoring while it is running could corrupt the world."
                )
                return@launch
            }
            _uiState.value = _uiState.value.copy(isRestoring = true, errorMessage = null)
            val result = worldBackupManager.restoreBackup(backupId) { progress ->
                _uiState.value = _uiState.value.copy(progress = progress)
            }

            when (result) {
                is Resource.Success -> {
                    loadWorldSize()
                    _uiState.value = _uiState.value.copy(isRestoring = false, progress = null)
                }
                is Resource.Error -> {
                    _uiState.value = _uiState.value.copy(isRestoring = false, progress = null, errorMessage = result.message)
                }
                else -> Unit
            }
        }
    }

    fun deleteBackup(backupId: String) {
        viewModelScope.launch {
            worldBackupManager.deleteBackup(backupId)
        }
    }
}

