package com.minehost.app.domain.repository

import com.minehost.app.core.common.ServerStatus
import com.minehost.app.domain.model.BackupModel
import com.minehost.app.domain.model.PlayerModel
import com.minehost.app.domain.model.PublicEndpoint
import com.minehost.app.domain.model.ServerLogEntry
import com.minehost.app.domain.model.ServerModel
import com.minehost.app.domain.model.WorldModel
import kotlinx.coroutines.flow.Flow

interface ServerRepository {
    fun getAllServers(): Flow<List<ServerModel>>
    suspend fun getServerById(id: String): ServerModel?
    fun getServerFlowById(id: String): Flow<ServerModel?>
    suspend fun saveServer(server: ServerModel)
    suspend fun updateStatus(id: String, status: ServerStatus)
    suspend fun updatePublicEndpoint(id: String, endpoint: PublicEndpoint?)
    suspend fun updateEulaConsent(id: String, accepted: Boolean)
    suspend fun deleteServer(id: String)

    /**
     * Run once when the app process starts. A server cannot still be running (or starting/stopping)
     * if the app process was just created, so any such status left in the database is stale: it is
     * marked CRASHED (it did not stop cleanly) and its public endpoint is cleared.
     * Returns how many servers were corrected.
     */
    suspend fun reconcileAfterProcessRestart(): Int
}

interface WorldRepository {
    fun getWorldsForServer(serverId: String): Flow<List<WorldModel>>
    suspend fun getWorldById(id: String): WorldModel?
    suspend fun saveWorld(world: WorldModel)
    suspend fun deleteWorld(world: WorldModel)
}

interface BackupRepository {
    fun getBackupsForServer(serverId: String): Flow<List<BackupModel>>
    suspend fun getBackupById(id: String): BackupModel?
    suspend fun saveBackup(backup: BackupModel)
    suspend fun deleteBackup(id: String)
}

interface PlayerRepository {
    fun getPlayersForServer(serverId: String): Flow<List<PlayerModel>>
    suspend fun savePlayer(player: PlayerModel)
    suspend fun deletePlayer(player: PlayerModel)
    /** Records that [playerName] was seen joining; keeps any existing op/ban/whitelist flags. */
    suspend fun recordJoin(serverId: String, playerName: String, joinedAt: Long)
    suspend fun setOp(serverId: String, playerName: String, isOp: Boolean)
    suspend fun setWhitelisted(serverId: String, playerName: String, isWhitelisted: Boolean)
    suspend fun setBanned(serverId: String, playerName: String, isBanned: Boolean)
}

interface LogRepository {
    fun getRecentLogs(serverId: String, limit: Int = 500): Flow<List<ServerLogEntry>>
    suspend fun log(serverId: String, message: String)
    suspend fun clearLogs(serverId: String)
}

