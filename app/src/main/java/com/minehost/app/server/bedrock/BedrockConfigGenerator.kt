package com.minehost.app.server.bedrock

import com.minehost.app.server.ServerConfiguration
import java.io.File

/**
 * Generates the two configuration files consumed by Axolotl-PM 5.x.
 * PocketMine uses "xbox-auth", not the Java-server "online-mode" property.
 */
object BedrockConfigGenerator {
    private val safeWorldName = Regex("^[A-Za-z0-9._-]{1,64}$")

    fun validate(config: ServerConfiguration): Result<Unit> = runCatching {
        require(config.localPort in 1..65534) {
            "Bedrock port must be between 1 and 65534 because the IPv6 port is configured as port + 1."
        }
        require(config.maxPlayers in 1..1000) { "Maximum players must be between 1 and 1000." }
        require(config.viewDistance in 2..32) { "View distance must be between 2 and 32 chunks." }
        require(config.maxMemoryMb >= 256) { "Bedrock memory limit must be at least 256 MB." }
        require(safeWorldName.matches(config.levelName)) {
            "World name may contain only letters, numbers, dots, underscores and hyphens (maximum 64 characters)."
        }
        require(config.serverName.isNotBlank()) { "Server name cannot be blank." }
    }

    private fun propertyValue(value: String): String =
        value.replace(Regex("[\\r\\n\\u0000-\\u001F\\u007F]"), " ").trim()

    private fun yamlQuoted(value: String): String = "\"${value}\""

    fun generateServerProperties(config: ServerConfiguration): String {
        validate(config).getOrThrow()
        val gamemodeNum = when (config.gameMode) {
            com.minehost.app.core.common.GameMode.SURVIVAL -> 0
            com.minehost.app.core.common.GameMode.CREATIVE -> 1
            com.minehost.app.core.common.GameMode.ADVENTURE -> 2
            com.minehost.app.core.common.GameMode.SPECTATOR -> 3
        }
        val difficultyNum = when (config.difficulty) {
            com.minehost.app.core.common.Difficulty.PEACEFUL -> 0
            com.minehost.app.core.common.Difficulty.EASY -> 1
            com.minehost.app.core.common.Difficulty.NORMAL -> 2
            com.minehost.app.core.common.Difficulty.HARD -> 3
        }
        val serverName = propertyValue(config.serverName)
        val motd = propertyValue(config.motd.ifBlank { config.serverName })

        return """
            # MineHost Bedrock Server Properties
            # Generated automatically by MineHost
            server-name=$serverName
            motd=$motd
            server-ip=0.0.0.0
            server-port=${config.localPort}
            enable-ipv6=on
            server-ipv6=::
            server-portv6=${config.localPort + 1}
            gamemode=$gamemodeNum
            difficulty=$difficultyNum
            max-players=${config.maxPlayers}
            white-list=off
            # NetherNet uses TCP signaling and UDP WebRTC on server-port; avoid a UDP query socket collision.
            enable-query=off
            enable-rcon=off
            view-distance=${config.viewDistance}
            pvp=${if (config.pvpEnabled) "on" else "off"}
            xbox-auth=${if (config.onlineMode) "on" else "off"}
            level-name=${config.levelName}
            level-type=DEFAULT
            auto-save=on
        """.trimIndent()
    }

    fun generatePocketMineYml(config: ServerConfiguration): String {
        validate(config).getOrThrow()
        return """
            # MineHost Axolotl-PM Configuration
            settings:
              language: "eng"
              enable-ansi: false
              async-workers: 2
            memory:
              global-limit: ${config.maxMemoryMb}M
              main-limit: ${config.maxMemoryMb - 128}M
            network:
              batch-threshold: 256
              compression-level: 6
              max-mtu-size: 1492
            level-settings:
              default-world: ${yamlQuoted(config.levelName)}
              auto-save: true
            transport:
              name: nethernet
              nethernet:
                key-file: nethernet.key
                ice-udp-mux: true
                port-range: [19132, 19132]
                mtu: 1200
                builtin-signaling:
                  enabled: true
                  port: 0
        """.trimIndent()
    }

    fun writeConfigurations(serverDir: File, config: ServerConfiguration) {
        validate(config).getOrThrow()
        check(serverDir.exists() || serverDir.mkdirs()) {
            "Could not create server directory: ${serverDir.absolutePath}"
        }
        File(serverDir, "server.properties").writeText(generateServerProperties(config))
        File(serverDir, "pocketmine.yml").writeText(generatePocketMineYml(config))
    }
}
