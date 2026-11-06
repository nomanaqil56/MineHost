package com.minehost.app

import com.minehost.app.core.common.LogLevel
import com.minehost.app.core.common.ServerStatus
import com.minehost.app.core.util.AnsiParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AnsiParserTest {

    @Test
    fun testStripAnsiCodes() {
        val coloredLog = "\u001B[32m[INFO] Server started successfully on port 19132\u001B[0m"
        val clean = AnsiParser.stripAnsi(coloredLog)
        assertEquals("[INFO] Server started successfully on port 19132", clean)
    }

    @Test
    fun testDetectLogLevel() {
        assertEquals(LogLevel.ERROR, AnsiParser.detectLogLevel("[ERROR] Could not bind to port 19132"))
        assertEquals(LogLevel.WARN, AnsiParser.detectLogLevel("[WARN] Can't keep up! Is the server overloaded?"))
        assertEquals(LogLevel.INFO, AnsiParser.detectLogLevel("[INFO] Preparing spawn area: 85%"))
        assertEquals(LogLevel.DEBUG, AnsiParser.detectLogLevel("[DEBUG] Sent packet 0x01"))
    }

    @Test
    fun testServerStatusTransitions() {
        assertTrue(ServerStatus.ONLINE.isRunning)
        assertTrue(ServerStatus.STARTING.isRunning)
        assertFalse(ServerStatus.STOPPED.isRunning)

        assertTrue(ServerStatus.STOPPED.canStart)
        assertTrue(ServerStatus.CRASHED.canStart)
        assertFalse(ServerStatus.ONLINE.canStart)

        assertTrue(ServerStatus.ONLINE.canStop)
        assertTrue(ServerStatus.STARTING.canStop)
        assertFalse(ServerStatus.STOPPED.canStop)
    }
}

