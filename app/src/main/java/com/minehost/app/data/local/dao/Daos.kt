package com.minehost.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.minehost.app.core.common.ServerStatus
import com.minehost.app.data.local.entity.BackupEntity
import com.minehost.app.data.local.entity.PlayerEntity
import com.minehost.app.data.local.entity.ServerEntity
import com.minehost.app.data.local.entity.ServerLogEntity
import com.minehost.app.data.local.entity.WorldEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ServerDao {
    @Query("SELECT * FROM servers ORDER BY createdAt DESC")
    fun getAllServers(): Flow<List<ServerEntity>>

    @Query("SELECT * FROM servers WHERE id = :id")
    suspend fun getServerById(id: String): ServerEntity?

    @Query("SELECT * FROM servers WHERE id = :id")
    fun getServerFlowById(id: String): Flow<ServerEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertServer(server: ServerEntity)

    @Update
    suspend fun updateServer(server: ServerEntity)

    @Query("UPDATE servers SET status = :status, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateStatus(id: String, status: ServerStatus, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE servers SET publicHostname = :hostname, publicPort = :port, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updatePublicEndpoint(id: String, hostname: String?, port: Int?, updatedAt: Long = System.currentTimeMillis())

    @Delete
    suspend fun deleteServer(server: ServerEntity)

    @Query("DELETE FROM servers WHERE id = :id")
    suspend fun deleteServerById(id: String)
}

@Dao
interface WorldDao {
    @Query("SELECT * FROM worlds WHERE serverId = :serverId ORDER BY createdAt DESC")
    fun getWorldsForServer(serverId: String): Flow<List<WorldEntity>>

    @Query("SELECT * FROM worlds WHERE id = :id")
    suspend fun getWorldById(id: String): WorldEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorld(world: WorldEntity)

    @Update
    suspend fun updateWorld(world: WorldEntity)

    @Delete
    suspend fun deleteWorld(world: WorldEntity)
}

@Dao
interface BackupDao {
    @Query("SELECT * FROM backups WHERE serverId = :serverId ORDER BY createdAt DESC")
    fun getBackupsForServer(serverId: String): Flow<List<BackupEntity>>

    @Query("SELECT * FROM backups WHERE id = :id")
    suspend fun getBackupById(id: String): BackupEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBackup(backup: BackupEntity)

    @Delete
    suspend fun deleteBackup(backup: BackupEntity)

    @Query("DELETE FROM backups WHERE id = :id")
    suspend fun deleteBackupById(id: String)
}

@Dao
interface PlayerDao {
    @Query("SELECT * FROM players WHERE serverId = :serverId ORDER BY name ASC")
    fun getPlayersForServer(serverId: String): Flow<List<PlayerEntity>>

    @Query("SELECT * FROM players WHERE serverId = :serverId AND name = :name LIMIT 1")
    suspend fun getPlayer(serverId: String, name: String): PlayerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlayer(player: PlayerEntity)

    @Update
    suspend fun updatePlayer(player: PlayerEntity)

    @Delete
    suspend fun deletePlayer(player: PlayerEntity)
}

@Dao
interface ServerLogDao {
    @Query("SELECT * FROM server_logs WHERE serverId = :serverId ORDER BY id DESC LIMIT :limit")
    fun getRecentLogs(serverId: String, limit: Int = 500): Flow<List<ServerLogEntity>>

    @Insert
    suspend fun insertLog(log: ServerLogEntity)

    @Query("DELETE FROM server_logs WHERE serverId = :serverId")
    suspend fun clearLogsForServer(serverId: String)

    @Query("DELETE FROM server_logs WHERE serverId = :serverId AND id NOT IN (SELECT id FROM server_logs WHERE serverId = :serverId ORDER BY id DESC LIMIT :keepCount)")
    suspend fun pruneLogs(serverId: String, keepCount: Int = 1000)
}
