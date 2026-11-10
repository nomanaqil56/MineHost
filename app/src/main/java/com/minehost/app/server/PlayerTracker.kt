package com.minehost.app.server

/**
 * Tracks who is online by reading the server's own console output.
 *
 * It only knows about joins/leaves it actually saw while MineHost was attached to the process, so it
 * must be [reset] when a server starts or stops. The log wording it looks for is
 * "<name> joined the game" / "<name> left the game" (Java and most Bedrock software) and
 * PocketMine-MP's "<name> [/ip:port] logged in with entity id ..." / "... logged out due to ...".
 *
 * LIMITATION: these patterns come from known server behaviour but have not been checked against a
 * running server on a device. If a server words these differently the count stays at what was seen,
 * so the UI shows it as a best-effort count from the console, never as an authoritative figure.
 */
class PlayerTracker {

    private val online = linkedSetOf<String>()

    private val ansi = Regex("\u001B\\[[0-9;]*[A-Za-z]")
    private val colorCodes = Regex("§.")
    private val joined = Regex("^(.{1,48}?) joined the game$")
    private val left = Regex("^(.{1,48}?) left the game$")
    private val pmJoined = Regex("^(.{1,48}?) \\[/?[0-9a-fA-F.:]+\\] logged in with entity id")
    private val pmLeft = Regex("^(.{1,48}?) \\[/?[0-9a-fA-F.:]+\\] logged out due to")

    /** Feed one console line. Returns true if the set of online players changed. */
    @Synchronized
    fun onLine(rawLine: String): Boolean {
        val text = message(rawLine)
        joined.find(text)?.let { return online.add(it.groupValues[1].trim()) }
        pmJoined.find(text)?.let { return online.add(it.groupValues[1].trim()) }
        left.find(text)?.let { return online.remove(it.groupValues[1].trim()) }
        pmLeft.find(text)?.let { return online.remove(it.groupValues[1].trim()) }
        return false
    }

    @Synchronized
    fun names(): Set<String> = online.toSet()

    @Synchronized
    fun reset() = online.clear()

    /** Strips colour codes and the "[time] [thread/LEVEL]:" prefix, leaving just the message. */
    private fun message(rawLine: String): String {
        var s = colorCodes.replace(ansi.replace(rawLine, ""), "").trim()
        val idx = s.lastIndexOf("]: ")
        if (idx >= 0) s = s.substring(idx + 3)
        return s.trim()
    }
}
