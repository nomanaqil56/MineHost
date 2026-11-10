package com.minehost.app.core.util

import com.minehost.app.core.common.LogLevel

object AnsiParser {
    // Regex pattern matching standard ANSI 7-bit and 8-bit escape codes
    private val ANSI_ESCAPE_REGEX = Regex("\u001B\\[[;?0-9]*[a-zA-Z]")

    /**
     * Strips all ANSI terminal escape sequences from the string.
     */
    fun stripAnsi(input: String): String {
        return ANSI_ESCAPE_REGEX.replace(input, "")
    }

    /**
     * Attempts to deduce the log level from log line keywords or formatting.
     */
    fun detectLogLevel(line: String): LogLevel {
        val upper = line.uppercase()
        return when {
            upper.contains("[ERROR]") || upper.contains("FATAL") || upper.contains("EXCEPTION") || upper.contains("CRITICAL") -> LogLevel.ERROR
            upper.contains("[WARN]") || upper.contains("WARNING") -> LogLevel.WARN
            upper.contains("[INFO]") || upper.contains("NOTICE") -> LogLevel.INFO
            upper.contains("[DEBUG]") -> LogLevel.DEBUG
            upper.contains("[TRACE]") || upper.contains("[VERBOSE]") -> LogLevel.VERBOSE
            else -> LogLevel.INFO
        }
    }
}

