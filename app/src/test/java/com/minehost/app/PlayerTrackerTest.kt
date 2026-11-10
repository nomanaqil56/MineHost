package com.minehost.app

import com.minehost.app.server.PlayerTracker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests of the parser against lines written in the formats the server software is known to
 * use. They are NOT captured from a running server; see PlayerTracker's LIMITATION note.
 */
class PlayerTrackerTest {

    @Test
    fun javaJoinAndLeave() {
        val t = PlayerTracker()
        assertTrue(t.onLine("[12:00:01] [Server thread/INFO]: Steve joined the game"))
        assertTrue(t.onLine("[12:00:05] [Server thread/INFO]: Alex joined the game"))
        assertEquals(setOf("Steve", "Alex"), t.names())
        assertTrue(t.onLine("[12:01:00] [Server thread/INFO]: Steve left the game"))
        assertEquals(setOf("Alex"), t.names())
    }

    @Test
    fun pocketMineLoginAndLogout() {
        val t = PlayerTracker()
        assertTrue(t.onLine("[12:00:01.123] [Server thread/INFO]: Steve [/192.168.1.5:54321] logged in with entity id 1 at (world, 0.5, 64.0, 0.5)"))
        assertEquals(setOf("Steve"), t.names())
        assertTrue(t.onLine("[12:02:00.001] [Server thread/INFO]: Steve [/192.168.1.5:54321] logged out due to client disconnect"))
        assertTrue(t.names().isEmpty())
    }

    @Test
    fun namesWithSpacesAndColourCodesAndAnsi() {
        val t = PlayerTracker()
        t.onLine("\u001B[32m[12:00:01] [Server thread/INFO]: §eCool Gamer joined the game\u001B[0m")
        assertEquals(setOf("Cool Gamer"), t.names())
    }

    @Test
    fun duplicateJoinDoesNotDoubleCountAndUnknownLeaveIsIgnored() {
        val t = PlayerTracker()
        assertTrue(t.onLine("Steve joined the game"))
        assertFalse(t.onLine("Steve joined the game"))
        assertEquals(1, t.names().size)
        assertFalse(t.onLine("Ghost left the game"))
    }

    @Test
    fun chatAndOtherLinesAreIgnored() {
        val t = PlayerTracker()
        assertFalse(t.onLine("[12:00:01] [Server thread/INFO]: <Steve> I joined the game yesterday lol"))
        assertFalse(t.onLine("Done (3.2s)! For help, type \"help\""))
        assertFalse(t.onLine(""))
        assertTrue(t.names().isEmpty())
    }

    @Test
    fun resetClearsEveryone() {
        val t = PlayerTracker()
        t.onLine("Steve joined the game")
        t.reset()
        assertTrue(t.names().isEmpty())
    }
}
