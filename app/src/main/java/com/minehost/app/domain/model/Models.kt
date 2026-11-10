package com.minehost.app.domain.model

import com.minehost.app.core.common.Difficulty
import com.minehost.app.core.common.GameMode
import com.minehost.app.core.common.LogLevel
import com.minehost.app.core.common.MinecraftEdition
import com.minehost.app.core.common.ServerStatus

data class ServerModel(
    val id: String,
    val name: String,
    val description: String,
    val edition: MinecraftEdition,
    val engineId: String,
    val engineVersion: String,
    val minecraftVersion: String,
    val worldId: String,
    val status: ServerStatus,
    val localPort: Int,
    val publicEndpoint: PublicEndpoint?,
    val maxPlayers: Int,
    val viewDistance: Int,
    val simulationDistance: Int,
    val minMemoryMb: Int,
    val maxMemoryMb: Int,
    val gameMode: GameMode,
    val difficulty: Difficulty,
    val pvpEnabled: Boolean,
    val onlineMode: Boolean,
    val autoStartTunnel: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
    val lastStartedAt: Long?,
    val lastStoppedAt: Long?,
    val eulaAccepted: Boolean = false,
    val eulaAcceptedAt: Long? = null
)

data class PublicEndpoint(
    val hostname: String,
    val port: Int,
    val protocol: String = "UDP/TCP"
) {
    val fullAddress: String
        get() = "$hostname:$port"
}

data class WorldModel(
    val id: String,
    val serverId: String,
    val name: String,
    val format: String,
    val seed: String?,
    val sizeBytes: Long,
    val createdAt: Long,
    val lastSavedAt: Long
)

data class BackupModel(
    val id: String,
    val serverId: String,
    val serverName: String,
    val worldName: String,
    val archivePath: String,
    val sizeBytes: Long,
    val checksumSha256: String,
    val createdAt: Long,
    val note: String?
)

data class PlayerModel(
    val id: String,
    val serverId: String,
    val name: String,
    val xuidOrUuid: String?,
    val isOp: Boolean,
    val isWhitelisted: Boolean,
    val isBanned: Boolean,
    val lastJoinedAt: Long?,
    val totalPlayTimeSeconds: Long
)

data class ServerLogEntry(
    val id: Long,
    val serverId: String,
    val timestamp: Long,
    val level: LogLevel,
    val message: String
)

