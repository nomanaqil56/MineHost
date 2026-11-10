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
    fun contentChecksumMatchesBackupChecksumAndDetectsTampering() {
        val sourceDir = tempFolder.newFolder("checksum_world")
        File(sourceDir, "level.dat").writeText("NBT")
        File(File(sourceDir, "db").apply { mkdirs() }, "000001.ldb").writeText("CHUNKS")

        val zip = File(tempFolder.root, "checksum_backup.zip")
        val created = ZipUtils.zipDirectory(sourceDir, zip)

        // Verification must reproduce exactly what backup creation recorded.
        assertEquals(created, ZipUtils.computeContentChecksum(zip))

        // An archive with different content must not match.
        val otherSource = tempFolder.newFolder("other_world")
        File(otherSource, "level.dat").writeText("DIFFERENT")
        val otherZip = File(tempFolder.root, "other_backup.zip")
        ZipUtils.zipDirectory(otherSource, otherZip)
        assertTrue(created != ZipUtils.computeContentChecksum(otherZip))
    }

    @Test
    fun additionalServerIdentityFileIsIncludedAndCoveredByChecksum() {
        val sourceDir = tempFolder.newFolder("identity_world")
        File(sourceDir, "level.dat").writeText("WORLD")
        val identityKey = File(tempFolder.root, "nethernet.key").apply { writeText("PRIVATE_SERVER_IDENTITY") }
        val zip = File(tempFolder.root, "identity_backup.zip")

        val created = ZipUtils.zipDirectory(
            sourceDir,
            zip,
            additionalFiles = mapOf("__minehost/nethernet.key" to identityKey)
        )

        assertEquals(created, ZipUtils.computeContentChecksum(zip))
        val restoredDir = tempFolder.newFolder("identity_restored")
        ZipUtils.unzip(zip, restoredDir)
        assertEquals(
            "PRIVATE_SERVER_IDENTITY",
            File(restoredDir, "__minehost/nethernet.key").readText()
        )
    }

    @Test
    fun additionalZipEntryRejectsPathTraversal() {
        val sourceDir = tempFolder.newFolder("identity_safe_world")
        File(sourceDir, "level.dat").writeText("WORLD")
        val identityKey = File(tempFolder.root, "unsafe.key").apply { writeText("KEY") }
        val zip = File(tempFolder.root, "unsafe_backup.zip")

        val result = runCatching {
            ZipUtils.zipDirectory(sourceDir, zip, additionalFiles = mapOf("../nethernet.key" to identityKey))
        }
        assertTrue(result.isFailure)
    }

    @Test
    fun truncatedArchiveFailsVerification() {
        val sourceDir = tempFolder.newFolder("trunc_world")
        File(sourceDir, "level.dat").writeText("A".repeat(50_000))
        val zip = File(tempFolder.root, "trunc_backup.zip")
        val created = ZipUtils.zipDirectory(sourceDir, zip)

        val bytes = zip.readBytes()
        val truncated = File(tempFolder.root, "trunc_cut.zip")
        truncated.writeBytes(bytes.copyOf(bytes.size / 2))

        // Either it throws (corrupt) or it yields a different checksum; it must never verify as intact.
        val verified = try {
            ZipUtils.computeContentChecksum(truncated) == created
        } catch (e: Exception) {
            false
        }
        assertTrue(!verified)
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

