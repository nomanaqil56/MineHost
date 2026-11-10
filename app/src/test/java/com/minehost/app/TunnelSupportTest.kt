package com.minehost.app

import com.minehost.app.networking.LogRedactor
import com.minehost.app.networking.PlayitOutputParser
import com.minehost.app.networking.ReachabilityTracker
import com.minehost.app.networking.ReconnectPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * UNIT TESTS of MineHost's own logic.
 *
 * NOTE: the sample agent lines below are ASSUMED formats, written from the patterns the code looks
 * for. They have NOT been captured from a real Playit agent, so these tests prove the parser
 * behaves as written, not that it matches the real agent's output. Verify on a device.
 */
class TunnelSupportTest {

    @Test
    fun reachabilityIsUnknownUntilProbed() {
        assertNull(ReachabilityTracker().reachable)
    }

    @Test
    fun oneFailureDoesNotFlipToUnreachableButTwoDo() {
        val t = ReachabilityTracker(failuresBeforeUnreachable = 2)
        assertEquals(true, t.record(true))
        assertEquals("a single dropped probe must not mark it unreachable", true, t.record(false))
        assertEquals(false, t.record(false))
        assertEquals("any success recovers immediately", true, t.record(true))
    }

    @Test
    fun failuresBeforeAnySuccessStayUnknownUntilThreshold() {
        val t = ReachabilityTracker(failuresBeforeUnreachable = 3)
        assertNull(t.record(false))
        assertNull(t.record(false))
        assertEquals(false, t.record(false))
        t.reset()
        assertNull(t.reachable)
    }

    @Test
    fun parserFindsClaimUrl() {
        val e = PlayitOutputParser.parse("Visit https://playit.gg/claim/abc123XYZ to setup your agent")
        assertEquals(PlayitOutputParser.Event.ClaimRequired("https://playit.gg/claim/abc123XYZ"), e)
    }

    @Test
    fun parserFindsEndpoint() {
        val e = PlayitOutputParser.parse("tunnel ready: eager-fox.gl.joinmc.link:41234 => 127.0.0.1:19132")
        assertEquals(PlayitOutputParser.Event.EndpointAssigned("eager-fox.gl.joinmc.link", 41234), e)
    }

    @Test
    fun parserIgnoresOrdinaryLinesAndLocalAddresses() {
        assertNull(PlayitOutputParser.parse("connecting to control server"))
        assertNull(PlayitOutputParser.parse("local server at 127.0.0.1:19132"))
        assertNull(PlayitOutputParser.parse(""))
    }

    @Test
    fun parserRejectsOutOfRangePorts() {
        assertNull(PlayitOutputParser.parse("x.joinmc.link:99999"))
        assertNull(PlayitOutputParser.parse("x.joinmc.link:0"))
    }

    @Test
    fun redactorHidesSecretsAndClaimCodes() {
        val secret = "a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6"
        val line = "agent secret_key=$secret loaded"
        val out = LogRedactor.redact(line)
        assertFalse(out.contains(secret))
        assertTrue(out.contains("[REDACTED]"))

        val claim = LogRedactor.redact("open https://playit.gg/claim/SuperSecretCode99 now")
        assertFalse(claim.contains("SuperSecretCode99"))
        assertTrue(claim.startsWith("open https://playit.gg/claim/"))

        val bearer = LogRedactor.redact("Authorization: Bearer abcdefghijklmnop12345")
        assertFalse(bearer.contains("abcdefghijklmnop12345"))
    }

    @Test
    fun redactorLeavesNormalTextAlone() {
        val text = "Tunnel established at eager-fox.gl.joinmc.link:41234"
        assertEquals(text, LogRedactor.redact(text))
    }

    @Test
    fun reconnectBackoffGrowsAndIsCapped() {
        assertEquals(5_000L, ReconnectPolicy.delayMs(1))
        assertEquals(10_000L, ReconnectPolicy.delayMs(2))
        assertEquals(20_000L, ReconnectPolicy.delayMs(3))
        assertEquals(40_000L, ReconnectPolicy.delayMs(4))
        assertEquals(60_000L, ReconnectPolicy.delayMs(5))
        assertEquals(60_000L, ReconnectPolicy.delayMs(50))
    }
}
