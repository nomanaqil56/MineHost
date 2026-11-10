package com.minehost.app.features.servers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.minehost.app.core.common.Difficulty
import com.minehost.app.core.common.GameMode
import com.minehost.app.core.common.MinecraftEdition
import com.minehost.app.core.common.Resource
import com.minehost.app.domain.usecase.CreateServerUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CreateServerFormState(
    val name: String = "",
    val description: String = "A MineHost server",
    val edition: MinecraftEdition = MinecraftEdition.BEDROCK,
    val engineId: String = "bedrock-pocketmine-v5",
    val engineVersion: String = "5.12.0",
    val minecraftVersion: String = "1.20.80",
    val port: Int = 19132,
    val maxPlayers: Int = 10,
    val viewDistance: Int = 10,
    val simulationDistance: Int = 4,
    val minMemoryMb: Int = 256,
    val maxMemoryMb: Int = 1024,
    val gameMode: GameMode = GameMode.SURVIVAL,
    val difficulty: Difficulty = Difficulty.NORMAL,
    val pvpEnabled: Boolean = true,
    val onlineMode: Boolean = true,
    val autoStartTunnel: Boolean = false,
    val currentStep: Int = 1,
    val errorMessage: String? = null,
    val isCreating: Boolean = false
)

@HiltViewModel
class CreateServerViewModel @Inject constructor(
    private val createServerUseCase: CreateServerUseCase
) : ViewModel() {

    private val _formState = MutableStateFlow(CreateServerFormState())
    val formState: StateFlow<CreateServerFormState> = _formState.asStateFlow()

    fun updateName(name: String) {
        _formState.value = _formState.value.copy(name = name, errorMessage = null)
    }

    fun updateDescription(desc: String) {
        _formState.value = _formState.value.copy(description = desc)
    }

    fun updateEdition(edition: MinecraftEdition) {
        val (engine, port) = when (edition) {
            MinecraftEdition.BEDROCK -> "bedrock-pocketmine-v5" to 19132
            MinecraftEdition.JAVA -> "java-paper" to 25565
            MinecraftEdition.CROSSPLAY -> "bedrock-pocketmine-v5" to 19132
        }
        _formState.value = _formState.value.copy(
            edition = edition,
            engineId = engine,
            port = port
        )
    }

    fun updatePort(port: Int) {
        _formState.value = _formState.value.copy(port = port)
    }

    fun updateMaxPlayers(max: Int) {
        _formState.value = _formState.value.copy(maxPlayers = max)
    }

    fun updateGameMode(mode: GameMode) {
        _formState.value = _formState.value.copy(gameMode = mode)
    }

    fun updateDifficulty(diff: Difficulty) {
        _formState.value = _formState.value.copy(difficulty = diff)
    }

    fun updateAutoStartTunnel(enabled: Boolean) {
        _formState.value = _formState.value.copy(autoStartTunnel = enabled)
    }

    fun nextStep() {
        val state = _formState.value
        if (state.currentStep == 1 && state.name.isBlank()) {
            _formState.value = state.copy(errorMessage = "Server name cannot be empty")
            return
        }
        if (state.currentStep < 4) {
            _formState.value = state.copy(currentStep = state.currentStep + 1, errorMessage = null)
        }
    }

    fun previousStep() {
        val state = _formState.value
        if (state.currentStep > 1) {
            _formState.value = state.copy(currentStep = state.currentStep - 1, errorMessage = null)
        }
    }

    fun createServer(onSuccess: (String) -> Unit) {
        val state = _formState.value
        if (state.name.isBlank()) {
            _formState.value = state.copy(errorMessage = "Server name cannot be empty")
            return
        }

        viewModelScope.launch {
            _formState.value = state.copy(isCreating = true, errorMessage = null)
            val result = createServerUseCase(
                name = state.name.trim(),
                description = state.description.trim(),
                edition = state.edition,
                engineId = state.engineId,
                engineVersion = state.engineVersion,
                minecraftVersion = state.minecraftVersion,
                localPort = state.port,
                maxPlayers = state.maxPlayers,
                viewDistance = state.viewDistance,
                simulationDistance = state.simulationDistance,
                minMemoryMb = state.minMemoryMb,
                maxMemoryMb = state.maxMemoryMb,
                gameMode = state.gameMode,
                difficulty = state.difficulty,
                pvpEnabled = state.pvpEnabled,
                onlineMode = state.onlineMode,
                autoStartTunnel = state.autoStartTunnel
            )

            when (result) {
                is Resource.Success -> {
                    _formState.value = state.copy(isCreating = false)
                    onSuccess(result.data.id)
                }
                is Resource.Error -> {
                    _formState.value = state.copy(isCreating = false, errorMessage = result.message)
                }
                else -> Unit
            }
        }
    }
}

