package com.minehost.app.networking

/**
 * Pure (Android-free) helpers for the tunnel layer so they can be unit-tested.
 */

/**
 * Turns individual probe results into a stable "reachable from outside" verdict.
 *
 * A single dropped UDP packet must not flip the UI to "unreachable", so the verdict only becomes
 * false after [failuresBeforeUnreachable] consecutive failures. Any success makes it true at once.
 * Until the first probe it is null ("not checked yet"), never a guess.
 */
class ReachabilityTracker(private val failuresBeforeUnreachable: Int = 2) {
    var reachable: Boolean? = null
        private set
    private var consecutiveFailures = 0

    fun record(success: Boolean): Boolean? {
        if (success) {
            consecutiveFailures = 0
            reachable = true
        } else {
            consecutiveFailures++
            if (consecutiveFailures >= failuresBeforeUnreachable) reachable = false
        }
        return reachable
    }

    fun reset() {
        consecutiveFailures = 0
        reachable = null
    }
}

/**
 * Separate facts about the tunnel path. They are deliberately not collapsed into one flag:
 * an agent can be running without being authenticated, authenticated without an assigned endpoint,
 * assigned an endpoint while the local server is down, and so on.
 *
 * Null means "not known yet" and must be shown as such, never as success.
 */
data class TunnelHealth(
    val agentRunning: Boolean = false,
    /** False while the agent is waiting for the user to claim it; null if unknown. */
    val authenticated: Boolean? = null,
    val endpointAssigned: Boolean = false,
    val localServerHealthy: Boolean? = null,
    val remoteReachable: Boolean? = null
)

/** Extracts facts from the Playit agent's console output. */
object PlayitOutputParser {

    private val claimUrlPattern = Regex("https://playit\\.gg/claim/[a-zA-Z0-9]+")
    private val endpointPattern =
        Regex("([a-zA-Z0-9\\-]+(?:\\.[a-zA-Z0-9\\-]+)*\\.(?:gl\\.joinmc\\.link|joinmc\\.link|ply\\.gg)):(\\d{1,5})")

    sealed class Event {
        data class ClaimRequired(val url: String) : Event()
        data class EndpointAssigned(val host: String, val port: Int) : Event()
    }

    /** Returns the first meaningful event on [line], or null for ordinary log chatter. */
    fun parse(line: String): Event? {
        claimUrlPattern.find(line)?.let { return Event.ClaimRequired(it.value) }
        endpointPattern.find(line)?.let { m ->
            val port = m.groupValues[2].toIntOrNull() ?: return null
            if (port !in 1..65535) return null
            return Event.EndpointAssigned(m.groupValues[1], port)
        }
        return null
    }
}

/** Removes secrets from text before it is stored or shown in diagnostics. */
object LogRedactor {

    private val claimCode = Regex("(https://playit\\.gg/claim/)[a-zA-Z0-9]+")
    private val keyedSecret =
        Regex("(?i)(secret[_ -]?key|secret|token|api[_ -]?key|authorization|password)([\"'\\s:=]+)(?:Bearer\\s+)?[A-Za-z0-9_\\-.=+/]{12,}")

    fun redact(text: String): String {
        var out = claimCode.replace(text) { it.groupValues[1] + "[REDACTED]" }
        out = keyedSecret.replace(out) { it.groupValues[1] + it.groupValues[2] + "[REDACTED]" }
        return out
    }
}

/** Exponential reconnect delays used when the agent exits unexpectedly. */
object ReconnectPolicy {
    const val MAX_ATTEMPTS = 5

    /** Delay before attempt number [attempt] (1-based): 5s, 10s, 20s, 40s, 60s (capped). */
    fun delayMs(attempt: Int): Long {
        require(attempt >= 1) { "attempt is 1-based" }
        val seconds = minOf(5L shl (attempt - 1).coerceAtMost(10), 60L)
        return seconds * 1000L
    }
}
