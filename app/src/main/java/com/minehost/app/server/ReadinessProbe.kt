package com.minehost.app.server

import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.EOFException
import java.io.InputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress
import java.net.Socket

/**
 * Protocol-level readiness probes for real Minecraft servers.
 *
 * A server is only considered ready when it answers a genuine Minecraft protocol request:
 *  - Bedrock: RakNet "Unconnected Ping" -> "Unconnected Pong" over UDP.
 *  - Java: Server List Ping (handshake + status request) -> JSON status response over TCP.
 *
 * Merely binding/not-binding a port, or the process being alive, is NOT proof of readiness.
 */
object ReadinessProbe {

    /** RakNet offline-message magic sequence. */
    val RAKNET_MAGIC: ByteArray = byteArrayOf(
        0x00, 0xFF.toByte(), 0xFF.toByte(), 0x00,
        0xFE.toByte(), 0xFE.toByte(), 0xFE.toByte(), 0xFE.toByte(),
        0xFD.toByte(), 0xFD.toByte(), 0xFD.toByte(), 0xFD.toByte(),
        0x12, 0x34, 0x56, 0x78
    )

    private const val ID_UNCONNECTED_PING = 0x01
    private const val ID_UNCONNECTED_PONG = 0x1C

    // ---------------------------------------------------------------- Bedrock (RakNet)

    fun buildRakNetPing(timeMillis: Long, clientGuid: Long): ByteArray {
        val out = ByteArrayOutputStream()
        out.write(ID_UNCONNECTED_PING)
        writeLongBE(out, timeMillis)
        out.write(RAKNET_MAGIC)
        writeLongBE(out, clientGuid)
        return out.toByteArray()
    }

    /** Pong layout: id(1) time(8) serverGuid(8) magic(16) stringLen(2) string(n). */
    fun isRakNetPong(data: ByteArray, length: Int): Boolean {
        if (length < 1 + 8 + 8 + 16 + 2) return false
        if ((data[0].toInt() and 0xFF) != ID_UNCONNECTED_PONG) return false
        for (i in RAKNET_MAGIC.indices) {
            if (data[17 + i] != RAKNET_MAGIC[i]) return false
        }
        return true
    }

    /** Returns true only if a RakNet server answers an unconnected ping on [host]:[port]. */
    fun probeBedrock(host: String, port: Int, timeoutMs: Int = 1500): Boolean {
        return try {
            DatagramSocket().use { socket ->
                socket.soTimeout = timeoutMs
                val ping = buildRakNetPing(System.currentTimeMillis(), 0x4D696E65486F7374L) // "MineHost"
                socket.send(DatagramPacket(ping, ping.size, InetSocketAddress(host, port)))
                val buf = ByteArray(2048)
                val reply = DatagramPacket(buf, buf.size)
                socket.receive(reply)
                isRakNetPong(buf, reply.length)
            }
        } catch (e: Exception) {
            false
        }
    }

    // ---------------------------------------------------------------- Java (Server List Ping)

    fun buildJavaHandshake(host: String, port: Int): ByteArray {
        val body = ByteArrayOutputStream()
        writeVarInt(body, 0x00)          // packet id: handshake
        writeVarInt(body, -1)            // protocol version: -1 = "just checking status"
        val hostBytes = host.toByteArray(Charsets.UTF_8)
        writeVarInt(body, hostBytes.size)
        body.write(hostBytes)
        body.write((port ushr 8) and 0xFF)
        body.write(port and 0xFF)
        writeVarInt(body, 1)             // next state: status
        return withLengthPrefix(body.toByteArray())
    }

    fun buildJavaStatusRequest(): ByteArray = withLengthPrefix(byteArrayOf(0x00))

    /**
     * Parses a status response. Valid iff the packet id is 0x00 and the payload is a JSON
     * object that carries a "version" field (every real Minecraft Java server includes it).
     */
    fun parseJavaStatusResponse(input: InputStream): Boolean {
        return try {
            val din = DataInputStream(input)
            val packetLength = readVarInt(din)
            if (packetLength <= 0 || packetLength > 1_000_000) return false
            val packetId = readVarInt(din)
            if (packetId != 0x00) return false
            val jsonLength = readVarInt(din)
            if (jsonLength <= 0 || jsonLength > 1_000_000) return false
            val json = ByteArray(jsonLength)
            din.readFully(json)
            val text = String(json, Charsets.UTF_8)
            text.trimStart().startsWith("{") && text.contains("\"version\"")
        } catch (e: Exception) {
            false
        }
    }

    /** Returns true only if a Minecraft Java server answers a Server List Ping. */
    fun probeJava(host: String, port: Int, timeoutMs: Int = 2000): Boolean {
        return try {
            Socket().use { socket ->
                socket.soTimeout = timeoutMs
                socket.connect(InetSocketAddress(host, port), timeoutMs)
                val out = socket.getOutputStream()
                out.write(buildJavaHandshake(host, port))
                out.write(buildJavaStatusRequest())
                out.flush()
                parseJavaStatusResponse(socket.getInputStream())
            }
        } catch (e: Exception) {
            false
        }
    }

    // ---------------------------------------------------------------- round-trip timing

    /** Round-trip time in ms of a successful RakNet ping, or null if the server did not answer. */
    fun measureBedrockMs(host: String, port: Int, timeoutMs: Int = 2000): Long? =
        timed { probeBedrock(host, port, timeoutMs) }

    /** Round-trip time in ms of a successful Server List Ping, or null if the server did not answer. */
    fun measureJavaMs(host: String, port: Int, timeoutMs: Int = 3000): Long? =
        timed { probeJava(host, port, timeoutMs) }

    private inline fun timed(probe: () -> Boolean): Long? {
        val start = System.nanoTime()
        return if (probe()) (System.nanoTime() - start) / 1_000_000L else null
    }

    // ---------------------------------------------------------------- helpers

    fun writeVarInt(out: ByteArrayOutputStream, value: Int) {
        var v = value
        while (true) {
            if ((v and 0x7F.inv()) == 0) {
                out.write(v)
                return
            }
            out.write((v and 0x7F) or 0x80)
            v = v ushr 7
        }
    }

    fun readVarInt(input: DataInputStream): Int {
        var result = 0
        var shift = 0
        while (true) {
            val b = input.read()
            if (b == -1) throw EOFException("Unexpected end of stream while reading VarInt")
            result = result or ((b and 0x7F) shl shift)
            if ((b and 0x80) == 0) return result
            shift += 7
            if (shift >= 35) throw IllegalArgumentException("VarInt is too long")
        }
    }

    private fun withLengthPrefix(payload: ByteArray): ByteArray {
        val out = ByteArrayOutputStream()
        writeVarInt(out, payload.size)
        out.write(payload)
        return out.toByteArray()
    }

    private fun writeLongBE(out: ByteArrayOutputStream, value: Long) {
        for (shift in 56 downTo 0 step 8) out.write(((value ushr shift) and 0xFF).toInt())
    }
}
