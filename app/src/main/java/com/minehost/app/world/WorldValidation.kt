package com.minehost.app.world

import com.minehost.app.core.common.MinecraftEdition
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.util.zip.ZipFile

sealed class WorldValidationResult {
    data object Valid : WorldValidationResult()
    data class Invalid(val reason: String) : WorldValidationResult()
}

object WorldValidation {

    /**
     * Validates an uncompressed world directory on disk.
     */
    fun validateWorldDirectory(worldDir: File, expectedEdition: MinecraftEdition): WorldValidationResult {
        if (!worldDir.exists() || !worldDir.isDirectory) {
            return WorldValidationResult.Invalid("World directory does not exist: ${worldDir.absolutePath}")
        }

        val levelDat = File(worldDir, "level.dat")
        if (!levelDat.exists() || levelDat.length() == 0L) {
            return WorldValidationResult.Invalid("Missing or empty level.dat file in world root")
        }

        when (expectedEdition) {
            MinecraftEdition.BEDROCK -> {
                val dbDir = File(worldDir, "db")
                // In new worlds db/ might not yet exist until first level generation, but if present must be a directory
                if (dbDir.exists() && !dbDir.isDirectory) {
                    return WorldValidationResult.Invalid("Bedrock 'db' must be a directory")
                }
            }
            MinecraftEdition.JAVA, MinecraftEdition.CROSSPLAY -> {
                val regionDir = File(worldDir, "region")
                if (regionDir.exists() && !regionDir.isDirectory) {
                    return WorldValidationResult.Invalid("Java 'region' must be a directory")
                }
            }
        }

        return WorldValidationResult.Valid
    }

    /**
     * Validates a ZIP archive or .mcworld file before importing.
     */
    fun validateWorldArchive(archiveFile: File, expectedEdition: MinecraftEdition): WorldValidationResult {
        if (!archiveFile.exists() || archiveFile.length() < 100) {
            return WorldValidationResult.Invalid("Archive file is missing or corrupted")
        }

        return try {
            ZipFile(archiveFile).use { zip ->
                val entries = zip.entries().asSequence().map { it.name.replace('\\', '/') }.toList()
                val hasLevelDat = entries.any { it.endsWith("level.dat") }
                if (!hasLevelDat) {
                    return WorldValidationResult.Invalid("Archive does not contain a level.dat file")
                }

                if (expectedEdition == MinecraftEdition.BEDROCK) {
                    val hasJavaRegion = entries.any { it.contains("region/") && it.endsWith(".mca") }
                    if (hasJavaRegion) {
                        return WorldValidationResult.Invalid("Archive appears to be a Java Edition world (contains .mca regions), but Bedrock server was selected")
                    }
                }

                WorldValidationResult.Valid
            }
        } catch (e: Exception) {
            WorldValidationResult.Invalid("Invalid ZIP archive format: ${e.message}")
        }
    }
}

