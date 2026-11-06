package com.minehost.app.world

import android.content.Context
import com.minehost.app.core.common.Resource
import com.minehost.app.core.logging.AppLogger
import com.minehost.app.core.util.FileUtils
import com.minehost.app.core.util.ZipUtils
import com.minehost.app.domain.model.BackupModel
import com.minehost.app.domain.repository.BackupRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class BackupManifest(
    val backupId: String,
    val serverId: String,
    val serverName: String,
    val worldName: String,
    val createdAt: Long,
    val sizeBytes: Long,
    val checksumSha256: String,
    val note: String? = null
)

@Singleton
class WorldBackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val worldManager: WorldManager,
    private val backupRepository: BackupRepository
) {
    private val json = Json { prettyPrint = true }

    /**
     * Creates an atomic backup of the target world.
     */
    suspend fun createBackup(
        serverId: String,
        serverName: String,
        worldName: String = "world",
        note: String? = null,
        onProgress: ((Float) -> Unit)? = null
    ): Resource<BackupModel> {
        return try {
            val worldDir = worldManager.getWorldDirectory(serverId, worldName)
            if (!worldDir.exists()) {
                return Resource.Error("World directory does not exist: ${worldDir.absolutePath}")
            }

            val backupsDir = worldManager.getBackupsDirectory(serverId)
            val timestampStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val backupId = "backup_${serverId.take(8)}_$timestampStr"
            val targetZip = File(backupsDir, "$backupId.zip")

            AppLogger.i("WorldBackupManager", "Creating backup to: ${targetZip.absolutePath}")
            val checksum = ZipUtils.zipDirectory(worldDir, targetZip, onProgress)

            val manifest = BackupManifest(
                backupId = backupId,
                serverId = serverId,
                serverName = serverName,
                worldName = worldName,
                createdAt = System.currentTimeMillis(),
                sizeBytes = targetZip.length(),
                checksumSha256 = checksum,
                note = note
            )

            // Write manifest alongside zip
            val manifestFile = File(backupsDir, "$backupId.json")
            manifestFile.writeText(json.encodeToString(manifest))

            val model = BackupModel(
                id = backupId,
                serverId = serverId,
                serverName = serverName,
                worldName = worldName,
                archivePath = targetZip.absolutePath,
                sizeBytes = targetZip.length(),
                checksumSha256 = checksum,
                createdAt = manifest.createdAt,
                note = note
            )
            backupRepository.saveBackup(model)

            AppLogger.i("WorldBackupManager", "Backup created successfully: $backupId")
            Resource.Success(model)
        } catch (e: Exception) {
            AppLogger.e("WorldBackupManager", "Backup creation failed: ${e.message}", e)
            Resource.Error("Backup failed: ${e.message}", e)
        }
    }

    /**
     * Restores a backup safely, preserving the current world in a fallback pre-restore snapshot.
     */
    suspend fun restoreBackup(
        backupId: String,
        onProgress: ((Float) -> Unit)? = null
    ): Resource<Unit> {
        return try {
            val backup = backupRepository.getBackupById(backupId)
                ?: return Resource.Error("Backup record not found: $backupId")

            val archiveFile = File(backup.archivePath)
            if (!archiveFile.exists()) {
                return Resource.Error("Backup archive file is missing from disk: ${archiveFile.absolutePath}")
            }

            val worldDir = worldManager.getWorldDirectory(backup.serverId, backup.worldName)
            val availableSpace = worldManager.getAvailableStorageBytes()
            if (availableSpace > 0 && availableSpace < archiveFile.length() * 2) {
                return Resource.Error("Insufficient free storage space to restore backup")
            }

            // Step 1: Create a safe pre-restore rollback snapshot of existing world if it has contents
            if (worldDir.exists() && worldDir.listFiles()?.isNotEmpty() == true) {
                val preRestoreZip = File(
                    worldManager.getBackupsDirectory(backup.serverId),
                    "pre_restore_${System.currentTimeMillis()}.zip"
                )
                ZipUtils.zipDirectory(worldDir, preRestoreZip)
            }

            // Step 2: Clean active world folder
            FileUtils.deleteDirectoryRecursively(worldDir)
            worldDir.mkdirs()

            // Step 3: Unzip backup archive into world folder
            ZipUtils.unzip(archiveFile, worldDir, onProgress)

            AppLogger.i("WorldBackupManager", "World restored successfully from: $backupId")
            Resource.Success(Unit)
        } catch (e: Exception) {
            AppLogger.e("WorldBackupManager", "Backup restoration failed: ${e.message}", e)
            Resource.Error("Restore failed: ${e.message}", e)
        }
    }

    /**
     * Deletes a backup archive and its manifest.
     */
    suspend fun deleteBackup(backupId: String): Resource<Unit> {
        return try {
            val backup = backupRepository.getBackupById(backupId)
            if (backup != null) {
                val archiveFile = File(backup.archivePath)
                if (archiveFile.exists()) archiveFile.delete()

                val manifestFile = File(archiveFile.parentFile, "$backupId.json")
                if (manifestFile.exists()) manifestFile.delete()

                backupRepository.deleteBackup(backupId)
            }
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error("Failed to delete backup: ${e.message}", e)
        }
    }
}

