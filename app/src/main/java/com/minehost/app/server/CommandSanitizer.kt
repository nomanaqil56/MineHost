package com.minehost.app.server

/**
 * Guards the server console's stdin.
 *
 * Commands are written to the server process one per line, so a value containing a line break
 * (for example a player name, pasted text or a crafted chat string that ends up in a "kick ..."
 * command) would otherwise execute extra commands. Anything with control characters is rejected
 * instead of being "cleaned", so a command never runs in a form the user did not type.
 */
object CommandSanitizer {

    /** Maximum accepted length of a single console command. */
    const val MAX_COMMAND_LENGTH = 1000

    fun isSafe(command: String): Boolean {
        if (command.isBlank()) return false
        if (command.length > MAX_COMMAND_LENGTH) return false
        return command.none { it.isISOControl() }
    }
}
