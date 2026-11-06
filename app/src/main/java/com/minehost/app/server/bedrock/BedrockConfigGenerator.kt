package com.minehost.app.server.bedrock

import com.minehost.app.server.ServerConfiguration
import java.io.File

object BedrockConfigGenerator {

    fun generateServerProperties(config: ServerConfiguration): String {
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

        return """
            # MineHost Bedrock Server Properties
            # Generated automatically by MineHost
            server-name=${config.serverName}
            motd=${config.motd.ifBlank { config.serverName }}
            server-port=${config.localPort}
            server-portv6=${config.localPort + 1}
            gamemode=$gamemodeNum
            difficulty=$difficultyNum
            max-players=${config.maxPlayers}
            white-list=off
            enable-query=on
            enable-rcon=off
            view-distance=${config.viewDistance}
            pvp=${if (config.pvpEnabled) "on" else "off"}
            online-mode=${if (config.onlineMode) "on" else "off"}
            allow-flight=${if (config.allowFlight) "on" else "off"}
            spawn-protection=${config.spawnProtection}
            level-name=${config.levelName}
            level-type=DEFAULT
            auto-save=on
        """.trimIndent()
    }

    fun generatePocketMineYml(config: ServerConfiguration): String {
        return """
            # PocketMine-MP Configuration File
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
              default-world: "${config.levelName}"
              auto-save: true
        """.trimIndent()
    }

    fun writeConfigurations(serverDir: File, config: ServerConfiguration) {
        serverDir.mkdirs()
        File(serverDir, "server.properties").writeText(generateServerProperties(config))
        File(serverDir, "pocketmine.yml").writeText(generatePocketMineYml(config))
    }
}
