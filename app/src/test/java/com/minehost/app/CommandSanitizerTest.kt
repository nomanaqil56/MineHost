package com.minehost.app

import com.minehost.app.server.CommandSanitizer
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CommandSanitizerTest {

    @Test
    fun ordinaryCommandsAreAccepted() {
        assertTrue(CommandSanitizer.isSafe("say hello"))
        assertTrue(CommandSanitizer.isSafe("kick Steve Reason: Disconnected by host"))
        assertTrue(CommandSanitizer.isSafe("whitelist add Alex_123"))
    }

    @Test
    fun lineBreaksAndControlCharactersAreRejected() {
        assertFalse(CommandSanitizer.isSafe("say hi\nop attacker"))
        assertFalse(CommandSanitizer.isSafe("say hi\rop attacker"))
        assertFalse(CommandSanitizer.isSafe("say hi\u0000"))
        assertFalse(CommandSanitizer.isSafe("kick Bob\nstop"))
    }

    @Test
    fun blankAndOversizedCommandsAreRejected() {
        assertFalse(CommandSanitizer.isSafe(""))
        assertFalse(CommandSanitizer.isSafe("   "))
        assertFalse(CommandSanitizer.isSafe("a".repeat(CommandSanitizer.MAX_COMMAND_LENGTH + 1)))
        assertTrue(CommandSanitizer.isSafe("a".repeat(CommandSanitizer.MAX_COMMAND_LENGTH)))
    }
}
