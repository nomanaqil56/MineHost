package com.minehost.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.minehost.app.core.common.Difficulty
import com.minehost.app.core.common.GameMode
import com.minehost.app.core.common.LogLevel
import com.minehost.app.core.common.MinecraftEdition
import com.minehost.app.core.common.ServerStatus

@Entity(tableName = "servers")
data class ServerEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String = "",
    val edition: MinecraftEdition,
    val engineId: String,
    val engineVersion: String,
    val minecraftVersion: String,
    val worldId: String,
    val status: ServerStatus = ServerStatus.STOPPED,
    val localPort: Int,
    val publicHostname: String? = null,
    val publicPort: Int? = null,
    val maxPlayers: Int = 10,
    val viewDistance: Int = 10,
    val simulationDistance: Int = 4,
    val minMemoryMb: Int = 256,
    val maxMemoryMb: Int = 1024,
    val gameMode: GameMode = GameMode.SURVIVAL,
    val difficulty: Difficulty = Difficulty.NORMAL,
    val pvpEnabled: Boolean = true,
    val onlineMode: Boolean = true,
    val autoStartTunnel: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val lastStartedAt: Long? = null,
    val lastStoppedAt: Long? = null
)

@Entity(
    tableName = "worlds",
    indices = [Index("serverId")]
)
data class WorldEntity(
    @PrimaryKey val id: String,
    val serverId: String,
    val name: String,
    val format: String, // "BEDROCK_LEVELDB" or "JAVA_ANVIL"
    val seed: String? = null,
    val sizeBytes: Long = 0L,
    val createdAt: Long = System.currentTimeMillis(),
    val lastSavedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "backups",
    indices = [Index("serverId")]
)
data class BackupEntity(
    @PrimaryKey val id: String,
    val serverId: String,
    val serverName: String,
    val worldName: String,
    val archivePath: String,
    val sizeBytes: Long,
    val checksumSha256: String,
    val createdAt: Long = System.currentTimeMillis(),
    val note: String? = null
)

@Entity(
    tableName = "players",
    indices = [Index(value = ["serverId", "name"], unique = true)]
)
data class PlayerEntity(
    @PrimaryKey val id: String,
    val serverId: String,
    val name: String,
    val xuidOrUuid: String? = null,
    val isOp: Boolean = false,
    val isWhitelisted: Boolean = false,
    val isBanned: Boolean = false,
    val lastJoinedAt: Long? = null,
    val totalPlayTimeSeconds: Long = 0L
)

@Entity(
    tableName = "server_logs",
    indices = [Index("serverId"), Index("timestamp")]
)
data class ServerLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val serverId: String,
    val timestamp: Long = System.currentTimeMillis(),
    val level: LogLevel,
    val message: String
)
