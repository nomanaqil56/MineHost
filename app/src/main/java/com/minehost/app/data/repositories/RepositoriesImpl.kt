package com.minehost.app.data.repositories

import com.minehost.app.core.common.ServerStatus
import com.minehost.app.core.util.AnsiParser
import com.minehost.app.data.local.dao.BackupDao
import com.minehost.app.data.local.dao.PlayerDao
import com.minehost.app.data.local.dao.ServerDao
import com.minehost.app.data.local.dao.ServerLogDao
import com.minehost.app.data.local.dao.WorldDao
import com.minehost.app.data.local.entity.PlayerEntity
import com.minehost.app.data.local.entity.ServerLogEntity
import com.minehost.app.domain.model.BackupModel
import com.minehost.app.domain.model.PlayerModel
import com.minehost.app.domain.model.PublicEndpoint
import com.minehost.app.domain.model.ServerLogEntry
import com.minehost.app.domain.model.ServerModel
import com.minehost.app.domain.model.WorldModel
import com.minehost.app.domain.repository.BackupRepository
import com.minehost.app.domain.repository.LogRepository
import com.minehost.app.domain.repository.PlayerRepository
import com.minehost.app.domain.repository.ServerRepository
import com.minehost.app.domain.repository.WorldRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ServerRepositoryImpl @Inject constructor(
    private val serverDao: ServerDao
) : ServerRepository {

    override fun getAllServers(): Flow<List<ServerModel>> {
        return serverDao.getAllServers().map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getServerById(id: String): ServerModel? {
        return serverDao.getServerById(id)?.toDomain()
    }

    override fun getServerFlowById(id: String): Flow<ServerModel?> {
        return serverDao.getServerFlowById(id).map { it?.toDomain() }
    }

    override suspend fun saveServer(server: ServerModel) {
        serverDao.insertServer(server.toEntity())
    }

    override suspend fun updateStatus(id: String, status: ServerStatus) {
        serverDao.updateStatus(id, status)
    }

    override suspend fun updatePublicEndpoint(id: String, endpoint: PublicEndpoint?) {
        serverDao.updatePublicEndpoint(id, endpoint?.hostname, endpoint?.port)
    }

    override suspend fun updateEulaConsent(id: String, accepted: Boolean) {
        serverDao.updateEula(id, accepted, if (accepted) System.currentTimeMillis() else null)
    }

    override suspend fun deleteServer(id: String) {
        serverDao.deleteServerById(id)
    }

    override suspend fun reconcileAfterProcessRestart(): Int {
        val staleStatuses = setOf(ServerStatus.ONLINE, ServerStatus.STARTING, ServerStatus.STOPPING)
        val stale = serverDao.getAllServers().first().filter { it.status in staleStatuses }
        stale.forEach {
            serverDao.updateStatus(it.id, ServerStatus.CRASHED)
            serverDao.updatePublicEndpoint(it.id, null, null)
        }
        return stale.size
    }
}

@Singleton
class WorldRepositoryImpl @Inject constructor(
    private val worldDao: WorldDao
) : WorldRepository {

    override fun getWorldsForServer(serverId: String): Flow<List<WorldModel>> {
        return worldDao.getWorldsForServer(serverId).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getWorldById(id: String): WorldModel? {
        return worldDao.getWorldById(id)?.toDomain()
    }

    override suspend fun saveWorld(world: WorldModel) {
        worldDao.insertWorld(world.toEntity())
    }

    override suspend fun deleteWorld(world: WorldModel) {
        worldDao.deleteWorld(world.toEntity())
    }
}

@Singleton
class BackupRepositoryImpl @Inject constructor(
    private val backupDao: BackupDao
) : BackupRepository {

    override fun getBackupsForServer(serverId: String): Flow<List<BackupModel>> {
        return backupDao.getBackupsForServer(serverId).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getBackupById(id: String): BackupModel? {
        return backupDao.getBackupById(id)?.toDomain()
    }

    override suspend fun saveBackup(backup: BackupModel) {
        backupDao.insertBackup(backup.toEntity())
    }

    override suspend fun deleteBackup(id: String) {
        backupDao.deleteBackupById(id)
    }
}

@Singleton
class PlayerRepositoryImpl @Inject constructor(
    private val playerDao: PlayerDao
) : PlayerRepository {

    override fun getPlayersForServer(serverId: String): Flow<List<PlayerModel>> {
        return playerDao.getPlayersForServer(serverId).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun savePlayer(player: PlayerModel) {
        playerDao.insertPlayer(player.toEntity())
    }

    override suspend fun deletePlayer(player: PlayerModel) {
        playerDao.deletePlayer(player.toEntity())
    }

    override suspend fun recordJoin(serverId: String, playerName: String, joinedAt: Long) {
        val existing = playerDao.getPlayer(serverId, playerName)
        if (existing != null) {
            playerDao.updatePlayer(existing.copy(lastJoinedAt = joinedAt))
        } else {
            playerDao.insertPlayer(
                PlayerEntity(
                    id = UUID.randomUUID().toString(),
                    serverId = serverId,
                    name = playerName,
                    lastJoinedAt = joinedAt
                )
            )
        }
    }

    override suspend fun setOp(serverId: String, playerName: String, isOp: Boolean) {
        val existing = playerDao.getPlayer(serverId, playerName)
        if (existing != null) {
            playerDao.updatePlayer(existing.copy(isOp = isOp))
        } else {
            playerDao.insertPlayer(
                PlayerEntity(
                    id = UUID.randomUUID().toString(),
                    serverId = serverId,
                    name = playerName,
                    isOp = isOp
                )
            )
        }
    }

    override suspend fun setWhitelisted(serverId: String, playerName: String, isWhitelisted: Boolean) {
        val existing = playerDao.getPlayer(serverId, playerName)
        if (existing != null) {
            playerDao.updatePlayer(existing.copy(isWhitelisted = isWhitelisted))
        } else {
            playerDao.insertPlayer(
                PlayerEntity(
                    id = UUID.randomUUID().toString(),
                    serverId = serverId,
                    name = playerName,
                    isWhitelisted = isWhitelisted
                )
            )
        }
    }

    override suspend fun setBanned(serverId: String, playerName: String, isBanned: Boolean) {
        val existing = playerDao.getPlayer(serverId, playerName)
        if (existing != null) {
            playerDao.updatePlayer(existing.copy(isBanned = isBanned))
        } else {
            playerDao.insertPlayer(
                PlayerEntity(
                    id = UUID.randomUUID().toString(),
                    serverId = serverId,
                    name = playerName,
                    isBanned = isBanned
                )
            )
        }
    }
}

@Singleton
class LogRepositoryImpl @Inject constructor(
    private val serverLogDao: ServerLogDao
) : LogRepository {

    override fun getRecentLogs(serverId: String, limit: Int): Flow<List<ServerLogEntry>> {
        return serverLogDao.getRecentLogs(serverId, limit).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun log(serverId: String, message: String) {
        val cleanMessage = AnsiParser.stripAnsi(message)
        val level = AnsiParser.detectLogLevel(cleanMessage)
        serverLogDao.insertLog(
            ServerLogEntity(
                serverId = serverId,
                timestamp = System.currentTimeMillis(),
                level = level,
                message = cleanMessage
            )
        )
        serverLogDao.pruneLogs(serverId, 1000)
    }

    override suspend fun clearLogs(serverId: String) {
        serverLogDao.clearLogsForServer(serverId)
    }
}

