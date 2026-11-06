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

    fun kickPlayer(playerName: String) {
        serverController.sendCommand("kick $playerName Reason: Disconnected by host")
    }

    fun toggleOp(player: PlayerModel) {
        val newOp = !player.isOp
        val cmd = if (newOp) "op ${player.name}" else "deop ${player.name}"
        serverController.sendCommand(cmd)
        viewModelScope.launch {
            playerRepository.setOp(serverId, player.name, newOp)
        }
    }

    fun toggleBan(player: PlayerModel) {
        val newBan = !player.isBanned
        val cmd = if (newBan) "ban ${player.name}" else "pardon ${player.name}"
        serverController.sendCommand(cmd)
        viewModelScope.launch {
            playerRepository.setBanned(serverId, player.name, newBan)
        }
    }

    fun toggleWhitelist(player: PlayerModel) {
        val newWhitelist = !player.isWhitelisted
        val cmd = if (newWhitelist) "whitelist add ${player.name}" else "whitelist remove ${player.name}"
        serverController.sendCommand(cmd)
        viewModelScope.launch {
            playerRepository.setWhitelisted(serverId, player.name, newWhitelist)
        }
    }
}
