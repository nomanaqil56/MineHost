package com.minehost.app

import com.minehost.app.server.ReadinessProbe
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.ServerSocket
import kotlin.concurrent.thread

/**
 * PROTOCOL UNIT TESTS ONLY.
 *
 * These verify MineHost's probe code against hand-built packets and tiny loopback responders.
 * They do NOT prove that a real PocketMine-MP or Paper server answers correctly; that requires
 * running the real server on a device (see docs/TESTING.md, "Real Minecraft tests").
 */
class ReadinessProbeTest {

    private fun buildPong(): ByteArray {
        val out = ByteArrayOutputStream()
        out.write(0x1C)
        out.write(ByteArray(8))               // echoed time
        out.write(ByteArray(8))               // server guid
        out.write(ReadinessProbe.RAKNET_MAGIC)
        val motd = "MCPE;MineHost;589;1.20.80;0;10;1;MineHost;Survival;1;19132;19133;".toByteArray()
        out.write((motd.size ushr 8) and 0xFF)
        out.write(motd.size and 0xFF)
        out.write(motd)
        return out.toByteArray()
    }

    @Test
    fun rakNetPingHasCorrectLayout() {
        val ping = ReadinessProbe.buildRakNetPing(timeMillis = 1L, clientGuid = 2L)
        assertEquals(1 + 8 + 16 + 8, ping.size)
        assertEquals(0x01, ping[0].toInt())
        assertArrayEquals(ReadinessProbe.RAKNET_MAGIC, ping.copyOfRange(9, 25))
    }

    @Test
    fun validPongIsRecognised() {
        val pong = buildPong()
        assertTrue(ReadinessProbe.isRakNetPong(pong, pong.size))
    }

    @Test
    fun invalidPongsAreRejected() {
        val pong = buildPong()
        // Wrong packet id
        val wrongId = pong.copyOf().also { it[0] = 0x01 }
        assertFalse(ReadinessProbe.isRakNetPong(wrongId, wrongId.size))
        // Corrupted magic
        val badMagic = pong.copyOf().also { it[20] = 0x7F }
        assertFalse(ReadinessProbe.isRakNetPong(badMagic, badMagic.size))
        // Truncated
        assertFalse(ReadinessProbe.isRakNetPong(pong, 10))
        // Declared MOTD length must match the actual datagram payload.
        val badLength = pong.copyOf().also { it[34] = (it[34] + 1).toByte() }
        assertFalse(ReadinessProbe.isRakNetPong(badLength, badLength.size))
        // Caller-supplied datagram length cannot exceed the backing array.
        assertFalse(ReadinessProbe.isRakNetPong(pong, pong.size + 1))
        // Garbage
        assertFalse(ReadinessProbe.isRakNetPong(ByteArray(64), 64))
    }

    @Test
    fun varIntRoundTrips() {
        for (value in listOf(0, 1, 127, 128, 255, 25565, 2_097_151, Int.MAX_VALUE, -1)) {
            val out = ByteArrayOutputStream()
            ReadinessProbe.writeVarInt(out, value)
            val decoded = ReadinessProbe.readVarInt(DataInputStream(ByteArrayInputStream(out.toByteArray())))
            assertEquals(value, decoded)
        }
    }

    private fun statusResponse(json: String, packetId: Int = 0x00): ByteArray {
        val payload = ByteArrayOutputStream()
        ReadinessProbe.writeVarInt(payload, packetId)
        val jsonBytes = json.toByteArray()
        ReadinessProbe.writeVarInt(payload, jsonBytes.size)
        payload.write(jsonBytes)
        val framed = ByteArrayOutputStream()
        ReadinessProbe.writeVarInt(framed, payload.size())
        framed.write(payload.toByteArray())
        return framed.toByteArray()
    }

    @Test
    fun validJavaStatusResponseIsAccepted() {
        val data = statusResponse("""{"version":{"name":"Paper 1.20.4","protocol":765},"players":{"max":20,"online":0}}""")
        assertTrue(ReadinessProbe.parseJavaStatusResponse(ByteArrayInputStream(data)))
    }

    @Test
    fun invalidJavaStatusResponsesAreRejected() {
        // Wrong packet id
        assertFalse(ReadinessProbe.parseJavaStatusResponse(ByteArrayInputStream(statusResponse("""{"version":{}}""", packetId = 0x01))))
        // JSON without version
        assertFalse(ReadinessProbe.parseJavaStatusResponse(ByteArrayInputStream(statusResponse("""{"hello":"world"}"""))))
        // Not JSON
        assertFalse(ReadinessProbe.parseJavaStatusResponse(ByteArrayInputStream(statusResponse("not json \"version\""))))
        // Empty / truncated stream
        assertFalse(ReadinessProbe.parseJavaStatusResponse(ByteArrayInputStream(ByteArray(0))))
        assertFalse(ReadinessProbe.parseJavaStatusResponse(ByteArrayInputStream(byteArrayOf(0x10, 0x00))))
        // Outer frame is shorter than the declared JSON body.
        val truncatedFrame = statusResponse("""{"version":{}}""").dropLast(1).toByteArray()
        assertFalse(ReadinessProbe.parseJavaStatusResponse(ByteArrayInputStream(truncatedFrame)))
        // Outer frame includes trailing data not accounted for by the JSON length.
        val framedWithTrailingGarbage = statusResponse("""{"version":{}}""").copyOf().also {
            it[0] = (it[0] + 1).toByte() // include one unaccounted byte inside the declared frame
        } + byteArrayOf(0x55)
        assertFalse(ReadinessProbe.parseJavaStatusResponse(ByteArrayInputStream(framedWithTrailingGarbage)))
        // A five-byte VarInt may use only the low four bits of its final byte.
        assertFalse(
            runCatching {
                ReadinessProbe.readVarInt(DataInputStream(ByteArrayInputStream(byteArrayOf(
                    0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0x7F
                ))))
            }.isSuccess
        )
    }

    @Test
    fun bedrockProbeSucceedsOnlyWhenSomethingAnswersWithAPong() {
        val server = DatagramSocket(0, java.net.InetAddress.getByName("127.0.0.1"))
        val port = server.localPort
        val responder = thread(start = true) {
            try {
                val buf = ByteArray(2048)
                val req = DatagramPacket(buf, buf.size)
                server.receive(req)
                val pong = buildPong()
                server.send(DatagramPacket(pong, pong.size, req.address, req.port))
            } catch (_: Exception) {
            }
        }
        try {
            assertTrue(ReadinessProbe.probeBedrock("127.0.0.1", port, timeoutMs = 2000))
        } finally {
            server.close()
            responder.join(2000)
        }
        // Port is now closed/unanswered: probe must report false, not "ready".
        assertFalse(ReadinessProbe.probeBedrock("127.0.0.1", port, timeoutMs = 300))
    }

    @Test
    fun bedrockProbeRejectsANonRakNetUdpService() {
        val server = DatagramSocket(0, java.net.InetAddress.getByName("127.0.0.1"))
        val port = server.localPort
        val responder = thread(start = true) {
            try {
                val buf = ByteArray(2048)
                val req = DatagramPacket(buf, buf.size)
                server.receive(req)
                val junk = ByteArray(40) { 0x55 }
                server.send(DatagramPacket(junk, junk.size, req.address, req.port))
            } catch (_: Exception) {
            }
        }
        try {
            assertFalse(ReadinessProbe.probeBedrock("127.0.0.1", port, timeoutMs = 1000))
        } finally {
            server.close()
            responder.join(2000)
        }
    }

    @Test
    fun javaProbeSucceedsAgainstAStatusResponderAndFailsAgainstAPlainOpenPort() {
        // Responder that speaks Server List Ping
        ServerSocket(0).use { good ->
            val t = thread(start = true) {
                try {
                    good.accept().use { s ->
                        s.soTimeout = 2000
                        val input = s.getInputStream()
                        input.read(ByteArray(256)) // swallow handshake + request
                        s.getOutputStream().write(statusResponse("""{"version":{"name":"x","protocol":1}}"""))
                        s.getOutputStream().flush()
                    }
                } catch (_: Exception) {
                }
            }
            assertTrue(ReadinessProbe.probeJava("127.0.0.1", good.localPort, timeoutMs = 2000))
            t.join(2000)
        }

        // An open TCP port that accepts but never speaks Minecraft must NOT count as ready.
        ServerSocket(0).use { silent ->
            val t = thread(start = true) {
                try {
                    silent.accept().use { Thread.sleep(1500) }
                } catch (_: Exception) {
                }
            }
            assertFalse(ReadinessProbe.probeJava("127.0.0.1", silent.localPort, timeoutMs = 500))
            t.join(3000)
        }
    }
}
