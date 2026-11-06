package com.minehost.app

import com.minehost.app.core.common.MinecraftEdition
import com.minehost.app.core.util.ZipUtils
import com.minehost.app.world.WorldValidation
import com.minehost.app.world.WorldValidationResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class ZipUtilsAndValidationTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun testZipAndUnzipRoundTrip() {
        val sourceDir = tempFolder.newFolder("source_world")
        File(sourceDir, "level.dat").writeText("DUMMY_NBT_HEADER")
        val dbDir = File(sourceDir, "db").apply { mkdirs() }
        File(dbDir, "000001.ldb").writeText("CHUNK_DATA_12345")

        val targetZip = File(tempFolder.root, "world_backup.zip")

        // Zip directory and get checksum
        val checksum = ZipUtils.zipDirectory(sourceDir, targetZip)
        assertTrue(targetZip.exists())
        assertTrue(checksum.isNotBlank())

        // Unzip into new folder
        val restoredDir = tempFolder.newFolder("restored_world")
        ZipUtils.unzip(targetZip, restoredDir)

        val restoredLevelDat = File(restoredDir, "level.dat")
        assertTrue(restoredLevelDat.exists())
        assertEquals("DUMMY_NBT_HEADER", restoredLevelDat.readText())

        val restoredChunk = File(restoredDir, "db/000001.ldb")
        assertTrue(restoredChunk.exists())
        assertEquals("CHUNK_DATA_12345", restoredChunk.readText())
    }

    @Test
    fun testWorldValidation() {
        val validWorld = tempFolder.newFolder("valid_bedrock")
        File(validWorld, "level.dat").writeText("VALID")
        File(validWorld, "db").mkdirs()

        val result = WorldValidation.validateWorldDirectory(validWorld, MinecraftEdition.BEDROCK)
        assertTrue(result is WorldValidationResult.Valid)

        val emptyWorld = tempFolder.newFolder("invalid_world")
        val invalidResult = WorldValidation.validateWorldDirectory(emptyWorld, MinecraftEdition.BEDROCK)
        assertTrue(invalidResult is WorldValidationResult.Invalid)
    }
}
