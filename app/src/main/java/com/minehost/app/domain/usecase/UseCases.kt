package com.minehost.app.domain.usecase

import com.minehost.app.core.common.Resource
import com.minehost.app.core.common.ServerStatus
import com.minehost.app.domain.model.BackupModel
import com.minehost.app.domain.model.ServerModel
import com.minehost.app.domain.model.WorldModel
import com.minehost.app.domain.repository.BackupRepository
import com.minehost.app.domain.repository.LogRepository
import com.minehost.app.domain.repository.ServerRepository
import com.minehost.app.domain.repository.WorldRepository
import com.minehost.app.world.WorldManager
import kotlinx.coroutines.flow.Flow
import java.io.File
import java.util.UUID
import javax.inject.Inject

class CreateServerUseCase @Inject constructor(
    private val serverRepository: ServerRepository,
    private val worldRepository: WorldRepository,
    private val worldManager: WorldManager
) {
    suspend operator fun invoke(
        name: String,
        description: String,
        edition: com.minehost.app.core.common.MinecraftEdition,
        engineId: String,
        engineVersion: String,
        minecraftVersion: String,
        localPort: Int,
        maxPlayers: Int,
        viewDistance: Int,
        simulationDistance: Int,
        minMemoryMb: Int,
        maxMemoryMb: Int,
        gameMode: com.minehost.app.core.common.GameMode,
        difficulty: com.minehost.app.core.common.Difficulty,
        pvpEnabled: Boolean,
        onlineMode: Boolean,
        autoStartTunnel: Boolean
    ): Resource<ServerModel> {
        return try {
            val serverId = UUID.randomUUID().toString()
            val worldId = UUID.randomUUID().toString()

            // Initialize world directory structure
            val worldDirResult = worldManager.createWorldDirectory(serverId, "world", edition)
            if (worldDirResult !is Resource.Success) {
                return Resource.Error("Failed to initialize server world directory")
            }

            val world = WorldModel(
                id = worldId,
                serverId = serverId,
                name = "world",
                format = if (edition == com.minehost.app.core.common.MinecraftEdition.BEDROCK) "BEDROCK_LEVELDB" else "JAVA_ANVIL",
                seed = null,
                sizeBytes = 0L,
                createdAt = System.currentTimeMillis(),
                lastSavedAt = System.currentTimeMillis()
            )
            worldRepository.saveWorld(world)

            val server = ServerModel(
                id = serverId,
                name = name,
                description = description,
                edition = edition,
                engineId = engineId,
                engineVersion = engineVersion,
                minecraftVersion = minecraftVersion,
                worldId = worldId,
                status = ServerStatus.STOPPED,
                localPort = localPort,
                publicEndpoint = null,
                maxPlayers = maxPlayers,
                viewDistance = viewDistance,
                simulationDistance = simulationDistance,
                minMemoryMb = minMemoryMb,
                maxMemoryMb = maxMemoryMb,
                gameMode = gameMode,
                difficulty = difficulty,
                pvpEnabled = pvpEnabled,
                onlineMode = onlineMode,
                autoStartTunnel = autoStartTunnel,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                lastStartedAt = null,
                lastStoppedAt = null
            )
            serverRepository.saveServer(server)

            Resource.Success(server)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to create server", e)
        }
    }
}

class GetServerLogsUseCase @Inject constructor(
    private val logRepository: LogRepository
) {
    operator fun invoke(serverId: String, limit: Int = 500) = logRepository.getRecentLogs(serverId, limit)
}

