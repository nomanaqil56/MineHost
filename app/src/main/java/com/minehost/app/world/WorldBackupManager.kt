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
    val note: String? = null,
    val includesServerIdentityKey: Boolean = false
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
            // NetherNet clients pin this private server identity key. Preserve it with the world
            // so restores/migrations do not unexpectedly change the server identity.
            val identityKey = File(worldManager.getServerDirectory(serverId), "nethernet.key")
            val additionalFiles = if (identityKey.isFile) {
                mapOf("__minehost/nethernet.key" to identityKey)
            } else {
                emptyMap()
            }
            val checksum = ZipUtils.zipDirectory(worldDir, targetZip, onProgress, additionalFiles)

            val manifest = BackupManifest(
                backupId = backupId,
                serverId = serverId,
                serverName = serverName,
                worldName = worldName,
                createdAt = System.currentTimeMillis(),
                sizeBytes = targetZip.length(),
                checksumSha256 = checksum,
                note = note,
                includesServerIdentityKey = additionalFiles.isNotEmpty()
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

            // Step 1: Verify archive integrity BEFORE touching the live world. Nothing is modified
            // if the archive is corrupt, truncated or does not match its recorded checksum.
            val actualChecksum = try {
                ZipUtils.computeContentChecksum(archiveFile)
            } catch (e: Exception) {
                return Resource.Error(
                    "Backup archive is corrupt or unreadable (${e.message}). Your current world was not changed."
                )
            }
            if (!actualChecksum.equals(backup.checksumSha256, ignoreCase = true)) {
                return Resource.Error(
                    "Backup checksum mismatch: the archive was modified or damaged since it was created. Your current world was not changed."
                )
            }

            // Step 2: Extract into a temporary directory next to the world (same filesystem, so the
            // final swap is a cheap rename). The live world is untouched while this runs.
            val parentDir = worldDir.parentFile
                ?: return Resource.Error("Cannot determine the world's parent directory. Your current world was not changed.")
            val stamp = System.currentTimeMillis()
            val tempDir = File(parentDir, ".restore_tmp_$stamp")
            val oldDir = File(parentDir, ".restore_old_$stamp")
            try {
                ZipUtils.unzip(archiveFile, tempDir, onProgress)
            } catch (e: Exception) {
                FileUtils.deleteDirectoryRecursively(tempDir)
                return Resource.Error("Restore failed while extracting the backup (${e.message}). Your current world was not changed.", e)
            }

            if (tempDir.listFiles().isNullOrEmpty()) {
                FileUtils.deleteDirectoryRecursively(tempDir)
                return Resource.Error("The backup archive contains no world data. Your current world was not changed.")
            }

            // Extract the optional NetherNet identity key from the archive metadata area. Stage it
            // outside the world tree, but do not replace the live key until the safety snapshot passes.
            val metadataDir = File(tempDir, "__minehost")
            val backupIdentityKey = File(metadataDir, "nethernet.key")
            val serverDir = worldManager.getServerDirectory(backup.serverId)
            val liveIdentityKey = File(serverDir, "nethernet.key")
            val stagedIdentityKey = File(serverDir, ".nethernet.key.restore-new-$stamp")
            val previousIdentityKey = File(serverDir, ".nethernet.key.restore-old-$stamp")
            val hasBackupIdentityKey = backupIdentityKey.isFile
            if (hasBackupIdentityKey) {
                try {
                    backupIdentityKey.copyTo(stagedIdentityKey, overwrite = true)
                } catch (e: Exception) {
                    stagedIdentityKey.delete()
                    FileUtils.deleteDirectoryRecursively(tempDir)
                    return Resource.Error("Could not stage the NetherNet server identity key: ${e.message}. Your world was not changed.", e)
                }
            }
            // Metadata is never restored inside the world directory.
            metadataDir.deleteRecursively()

            // Step 3: Safety snapshot of the existing world. If it cannot be made, abort.
            if (worldDir.exists() && worldDir.listFiles()?.isNotEmpty() == true) {
                val preRestoreZip = File(
                    worldManager.getBackupsDirectory(backup.serverId),
                    "pre_restore_$stamp.zip"
                )
                try {
                    ZipUtils.zipDirectory(worldDir, preRestoreZip)
                } catch (e: Exception) {
                    preRestoreZip.delete()
                    FileUtils.deleteDirectoryRecursively(tempDir)
                    return Resource.Error("Could not create a safety snapshot of the current world (${e.message}). Restore cancelled; nothing was changed.", e)
                }
            }

            // Step 4: Install the backup identity key only after the current world safety snapshot
            // succeeded. Keep the previous key until the world swap is committed.
            var identityKeyUpdated = false
            if (hasBackupIdentityKey) {
                try {
                    if (liveIdentityKey.isFile) {
                        liveIdentityKey.copyTo(previousIdentityKey, overwrite = true)
                    }
                    if (liveIdentityKey.exists() && !liveIdentityKey.delete()) {
                        throw IOException("Could not stage the existing NetherNet identity key.")
                    }
                    if (!stagedIdentityKey.renameTo(liveIdentityKey)) {
                        if (previousIdentityKey.isFile) {
                            previousIdentityKey.copyTo(liveIdentityKey, overwrite = true)
                        }
                        throw IOException("Could not install the restored NetherNet identity key.")
                    }
                    liveIdentityKey.setReadable(false, false)
                    liveIdentityKey.setWritable(false, false)
                    liveIdentityKey.setReadable(true, true)
                    liveIdentityKey.setWritable(true, true)
                    identityKeyUpdated = true
                } catch (e: Exception) {
                    stagedIdentityKey.delete()
                    if (!liveIdentityKey.exists() && previousIdentityKey.isFile) {
                        previousIdentityKey.copyTo(liveIdentityKey, overwrite = true)
                    }
                    previousIdentityKey.delete()
                    FileUtils.deleteDirectoryRecursively(tempDir)
                    return Resource.Error("Could not restore the NetherNet server identity key: ${e.message}. Your world was not changed.", e)
                }
            }

            // Step 5: Swap with rollback (see WorldSwap). The old world is only deleted after the
            // verified replacement is in place.
            when (val swap = WorldSwap.replace(worldDir, tempDir, oldDir, FileUtils::deleteDirectoryRecursively)) {
                is WorldSwap.Result.Failed -> {
                    if (identityKeyUpdated) {
                        liveIdentityKey.delete()
                        if (previousIdentityKey.isFile) {
                            previousIdentityKey.copyTo(liveIdentityKey, overwrite = true)
                        }
                    }
                    stagedIdentityKey.delete()
                    previousIdentityKey.delete()
                    return Resource.Error(swap.message)
                }
                WorldSwap.Result.Success -> {
                    previousIdentityKey.delete()
                    stagedIdentityKey.delete()
                }
            }

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

