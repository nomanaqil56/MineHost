package com.minehost.app.world

import android.content.Context
import com.minehost.app.core.common.Constants
import com.minehost.app.core.common.MinecraftEdition
import com.minehost.app.core.common.Resource
import com.minehost.app.core.logging.AppLogger
import com.minehost.app.core.util.FileUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorldManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val baseDir: File by lazy {
        File(context.filesDir, Constants.DIR_MINEHOST).apply { if (!exists()) mkdirs() }
    }

    fun getServerDirectory(serverId: String): File {
        return File(baseDir, "${Constants.DIR_SERVERS}/$serverId").apply { if (!exists()) mkdirs() }
    }

    fun getWorldDirectory(serverId: String, worldName: String = "world"): File {
        return File(getServerDirectory(serverId), "${Constants.DIR_WORLDS}/$worldName").apply {
            if (!exists()) mkdirs()
        }
    }

    fun getBackupsDirectory(serverId: String): File {
        return File(getServerDirectory(serverId), Constants.DIR_BACKUPS).apply {
            if (!exists()) mkdirs()
        }
    }

    fun createWorldDirectory(serverId: String, worldName: String, edition: MinecraftEdition): Resource<File> {
        return try {
            val worldDir = getWorldDirectory(serverId, worldName)
            if (edition == MinecraftEdition.BEDROCK) {
                File(worldDir, "db").mkdirs()
            } else {
                File(worldDir, "region").mkdirs()
            }
            Resource.Success(worldDir)
        } catch (e: Exception) {
            AppLogger.e("WorldManager", "Failed to create world directory: ${e.message}", e)
            Resource.Error("Could not create world directory", e)
        }
    }

    fun calculateWorldSize(serverId: String, worldName: String = "world"): Long {
        val worldDir = getWorldDirectory(serverId, worldName)
        return FileUtils.calculateDirectorySize(worldDir)
    }

    fun getAvailableStorageBytes(): Long {
        return FileUtils.getAvailableStorageBytes(baseDir)
    }
}
