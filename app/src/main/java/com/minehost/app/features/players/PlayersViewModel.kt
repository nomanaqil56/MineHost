package com.minehost.app.features.players

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.minehost.app.domain.model.PlayerModel
import com.minehost.app.domain.repository.PlayerRepository
import com.minehost.app.server.ServerController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayersViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val playerRepository: PlayerRepository,
    private val serverController: ServerController
) : ViewModel() {

    val serverId: String = checkNotNull(savedStateHandle["serverId"])

    val players: StateFlow<List<PlayerModel>> = playerRepository.getPlayersForServer(serverId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _message = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)
    /** Explains why an action did nothing (for example the server is not running). */
    val message: StateFlow<String?> = _message

    fun dismissMessage() { _message.value = null }

    /** Sends [command]; returns false (and tells the user why) if it could not be delivered. */
    private fun send(command: String): Boolean {
        val running = serverController.activeServerId.value == serverId &&
            serverController.serverStatus.value == com.minehost.app.core.common.ServerStatus.ONLINE
        if (!running) {
            _message.value = "Start the server first: player commands are sent to the running server."
            return false
        }
        if (!serverController.sendCommand(command)) {
            _message.value = "The server did not accept that command. The player name may contain unsupported characters."
            return false
        }
        return true
    }

    fun kickPlayer(playerName: String) {
        send("kick $playerName Reason: Disconnected by host")
    }

    fun toggleOp(player: PlayerModel) {
        val newOp = !player.isOp
        if (!send(if (newOp) "op ${player.name}" else "deop ${player.name}")) return
        viewModelScope.launch { playerRepository.setOp(serverId, player.name, newOp) }
    }

    fun toggleBan(player: PlayerModel) {
        val newBan = !player.isBanned
        if (!send(if (newBan) "ban ${player.name}" else "pardon ${player.name}")) return
        viewModelScope.launch { playerRepository.setBanned(serverId, player.name, newBan) }
    }

    fun toggleWhitelist(player: PlayerModel) {
        val newWhitelist = !player.isWhitelisted
        if (!send(if (newWhitelist) "whitelist add ${player.name}" else "whitelist remove ${player.name}")) return
        viewModelScope.launch { playerRepository.setWhitelisted(serverId, player.name, newWhitelist) }
    }
}
