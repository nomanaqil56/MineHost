package com.minehost.app.core.common

enum class ServerStatus {
    NOT_CREATED,
    INSTALLING,
    STOPPED,
    STARTING,
    ONLINE,
    STOPPING,
    CRASHED,
    ERROR;

    val isRunning: Boolean
        get() = this == ONLINE || this == STARTING || this == STOPPING

    val canStart: Boolean
        get() = this == STOPPED || this == CRASHED || this == ERROR

    val canStop: Boolean
        get() = this == ONLINE || this == STARTING
}

enum class MinecraftEdition {
    BEDROCK,
    JAVA,
    CROSSPLAY;

    val defaultPort: Int
        get() = when (this) {
            BEDROCK -> 19132
            JAVA -> 25565
            CROSSPLAY -> 19132
        }

    val defaultProtocol: NetworkProtocol
        get() = when (this) {
            BEDROCK -> NetworkProtocol.UDP
            JAVA -> NetworkProtocol.TCP
            CROSSPLAY -> NetworkProtocol.BOTH
        }
}

enum class TunnelStatus {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    RECONNECTING,
    ERROR
}

enum class NetworkProtocol {
    UDP,
    TCP,
    BOTH
}

enum class LogLevel {
    VERBOSE,
    DEBUG,
    INFO,
    WARN,
    ERROR,
    SYSTEM
}

enum class GameMode {
    SURVIVAL,
    CREATIVE,
    ADVENTURE,
    SPECTATOR
}

enum class Difficulty {
    PEACEFUL,
    EASY,
    NORMAL,
    HARD
}
