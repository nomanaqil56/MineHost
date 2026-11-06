package com.minehost.app.features.console

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.minehost.app.domain.model.ServerLogEntry
import com.minehost.app.domain.usecase.GetServerLogsUseCase
import com.minehost.app.server.ServerController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ConsoleUiState(
    val logs: List<ServerLogEntry> = emptyList(),
    val isRunning: Boolean = false,
    val commandText: String = "",
    val commandHistory: List<String> = emptyList()
)

@HiltViewModel
class ConsoleViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    getServerLogsUseCase: GetServerLogsUseCase,
    private val serverController: ServerController
) : ViewModel() {

    val serverId: String = checkNotNull(savedStateHandle["serverId"])

    val logs: StateFlow<List<ServerLogEntry>> = getServerLogsUseCase(serverId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _commandText = MutableStateFlow("")
    val commandText: StateFlow<String> = _commandText.asStateFlow()

    fun updateCommand(text: String) {
        _commandText.value = text
    }

    fun sendCommand() {
        val cmd = _commandText.value.trim()
        if (cmd.isNotEmpty()) {
            serverController.sendCommand(cmd)
            _commandText.value = ""
        }
    }
}
