package com.minehost.app.data.repositories

import com.minehost.app.core.util.AnsiParser
import com.minehost.app.data.local.dao.BackupDao
import com.minehost.app.data.local.dao.PlayerDao
import com.minehost.app.data.local.dao.ServerDao
import com.minehost.app.data.local.dao.ServerLogDao
import com.minehost.app.data.local.dao.WorldDao
import com.minehost.app.data.local.entity.BackupEntity
import com.minehost.app.data.local.entity.PlayerEntity
import com.minehost.app.data.local.entity.ServerEntity
import com.minehost.app.data.local.entity.ServerLogEntity
import com.minehost.app.data.local.entity.WorldEntity
import com.minehost.app.domain.model.BackupModel
import com.minehost.app.domain.model.PlayerModel
import com.minehost.app.domain.model.PublicEndpoint
import com.minehost.app.domain.model.ServerLogEntry
import com.minehost.app.domain.model.ServerModel
import com.minehost.app.domain.model.WorldModel

fun ServerEntity.toDomain(): ServerModel {
    val endpoint = if (publicHostname != null && publicPort != null) {
        PublicEndpoint(publicHostname, publicPort)
    } else null

    return ServerModel(
        id = id,
        name = name,
        description = description,
        edition = edition,
        engineId = engineId,
        engineVersion = engineVersion,
        minecraftVersion = minecraftVersion,
        worldId = worldId,
        status = status,
        localPort = localPort,
        publicEndpoint = endpoint,
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
        createdAt = createdAt,
        updatedAt = updatedAt,
        lastStartedAt = lastStartedAt,
        lastStoppedAt = lastStoppedAt,
        eulaAccepted = eulaAccepted,
        eulaAcceptedAt = eulaAcceptedAt
    )
}

fun ServerModel.toEntity(): ServerEntity {
    return ServerEntity(
        id = id,
        name = name,
        description = description,
        edition = edition,
        engineId = engineId,
        engineVersion = engineVersion,
        minecraftVersion = minecraftVersion,
        worldId = worldId,
        status = status,
        localPort = localPort,
        publicHostname = publicEndpoint?.hostname,
        publicPort = publicEndpoint?.port,
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
        createdAt = createdAt,
        updatedAt = updatedAt,
        lastStartedAt = lastStartedAt,
        lastStoppedAt = lastStoppedAt,
        eulaAccepted = eulaAccepted,
        eulaAcceptedAt = eulaAcceptedAt
    )
}

fun WorldEntity.toDomain() = WorldModel(id, serverId, name, format, seed, sizeBytes, createdAt, lastSavedAt)
fun WorldModel.toEntity() = WorldEntity(id, serverId, name, format, seed, sizeBytes, createdAt, lastSavedAt)

fun BackupEntity.toDomain() = BackupModel(id, serverId, serverName, worldName, archivePath, sizeBytes, checksumSha256, createdAt, note)
fun BackupModel.toEntity() = BackupEntity(id, serverId, serverName, worldName, archivePath, sizeBytes, checksumSha256, createdAt, note)

fun PlayerEntity.toDomain() = PlayerModel(id, serverId, name, xuidOrUuid, isOp, isWhitelisted, isBanned, lastJoinedAt, totalPlayTimeSeconds)
fun PlayerModel.toEntity() = PlayerEntity(id, serverId, name, xuidOrUuid, isOp, isWhitelisted, isBanned, lastJoinedAt, totalPlayTimeSeconds)

fun ServerLogEntity.toDomain() = ServerLogEntry(id, serverId, timestamp, level, message)

