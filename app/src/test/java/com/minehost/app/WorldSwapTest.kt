package com.minehost.app

import com.minehost.app.world.WorldSwap
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/** Real filesystem tests of the restore swap. These exercise real renames, not mocks. */
class WorldSwapTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private fun dirWith(parent: File, name: String, content: String): File {
        val d = File(parent, name).apply { mkdirs() }
        File(d, "level.dat").writeText(content)
        return d
    }

    @Test
    fun replacesWorldAndRemovesOldCopy() {
        val parent = tmp.newFolder("p1")
        val world = dirWith(parent, "world", "OLD")
        val replacement = dirWith(parent, ".restore_tmp", "NEW")
        val aside = File(parent, ".restore_old")

        val result = WorldSwap.replace(world, replacement, aside)

        assertEquals(WorldSwap.Result.Success, result)
        assertEquals("NEW", File(world, "level.dat").readText())
        assertFalse("old world must be cleaned up", aside.exists())
        assertFalse("temp dir must be consumed", replacement.exists())
    }

    @Test
    fun worksWhenThereIsNoExistingWorld() {
        val parent = tmp.newFolder("p2")
        val world = File(parent, "world") // does not exist
        val replacement = dirWith(parent, ".restore_tmp", "NEW")

        val result = WorldSwap.replace(world, replacement, File(parent, ".restore_old"))

        assertEquals(WorldSwap.Result.Success, result)
        assertEquals("NEW", File(world, "level.dat").readText())
    }

    @Test
    fun rollsBackWhenReplacementCannotBeMovedIntoPlace() {
        val parent = tmp.newFolder("p3")
        val world = dirWith(parent, "world", "OLD")
        val missingReplacement = File(parent, ".restore_tmp_never_created") // rename of this will fail
        val aside = File(parent, ".restore_old")

        val result = WorldSwap.replace(world, missingReplacement, aside)

        assertTrue(result is WorldSwap.Result.Failed)
        assertTrue((result as WorldSwap.Result.Failed).message.contains("previous world was put back"))
        assertEquals("the original world must be intact", "OLD", File(world, "level.dat").readText())
        assertFalse(aside.exists())
    }

    @Test
    fun oldWorldIsNotDeletedBeforeTheNewOneIsInPlace() {
        val parent = tmp.newFolder("p4")
        val world = dirWith(parent, "world", "OLD")
        val replacement = dirWith(parent, ".restore_tmp", "NEW")
        val aside = File(parent, ".restore_old")
        val deleted = mutableListOf<String>()
        var newWorldPresentWhenOldDeleted = false

        WorldSwap.replace(world, replacement, aside) { dir ->
            deleted += dir.name
            if (dir == aside) newWorldPresentWhenOldDeleted = File(world, "level.dat").readText() == "NEW"
            dir.deleteRecursively()
        }

        assertEquals(listOf(".restore_old"), deleted)
        assertTrue("old world may only be deleted once the new one is in place", newWorldPresentWhenOldDeleted)
    }
}
