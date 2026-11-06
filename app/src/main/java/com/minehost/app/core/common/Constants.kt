package com.minehost.app.core.common

object Constants {
    const val DATABASE_NAME = "minehost_database.db"
    const val DATASTORE_NAME = "minehost_preferences"

    // Storage Paths
    const val DIR_MINEHOST = "minehost"
    const val DIR_SERVERS = "servers"
    const val DIR_ENGINES = "engines"
    const val DIR_RUNTIMES = "runtimes"
    const val DIR_TUNNELS = "tunnels"
    const val DIR_WORLDS = "worlds"
    const val DIR_BACKUPS = "backups"
    const val DIR_LOGS = "logs"
    const val DIR_PLUGINS = "plugins"

    // Default Server Configurations
    const val DEFAULT_BEDROCK_PORT = 19132
    const val DEFAULT_JAVA_PORT = 25565
    const val DEFAULT_MAX_PLAYERS = 10
    const val DEFAULT_VIEW_DISTANCE = 10
    const val DEFAULT_SIMULATION_DISTANCE = 4

    // Default Memory
    const val DEFAULT_MIN_MEMORY_MB = 256
    const val DEFAULT_MAX_MEMORY_MB = 1024

    // Timeouts
    const val SERVER_STARTUP_TIMEOUT_MS = 60_000L
    const val SERVER_SHUTDOWN_TIMEOUT_MS = 15_000L
    const val TUNNEL_CONNECT_TIMEOUT_MS = 30_000L
}

sealed class Resource<out T> {
    data class Success<out T>(val data: T) : Resource<T>()
    data class Error(val message: String, val cause: Throwable? = null) : Resource<Nothing>()
    data class Loading(val progress: Float? = null, val message: String? = null) : Resource<Nothing>()
}

