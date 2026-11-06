package com.minehost.app.server.geyser

import java.io.File

object GeyserConfigGenerator {

    fun generateGeyserConfig(
        bedrockPort: Int = 19132,
        remoteJavaPort: Int = 25565,
        serverName: String = "MineHost CrossPlay",
        motd: String = "MineHost Bedrock Bridge",
        authType: String = "floodgate"
    ): String {
        return """
            # Geyser-Standalone Configuration
            bedrock:
              address: 0.0.0.0
              port: $bedrockPort
              motd1: "$serverName"
              motd2: "$motd"
              broadcast-options:
                broadcast-to-lan: true
            remote:
              address: 127.0.0.1
              port: $remoteJavaPort
              auth-type: $authType
            floodgate:
              key-file: key.pem
            metrics:
              enabled: false
        """.trimIndent()
    }

    fun writeGeyserConfig(targetDir: File, bedrockPort: Int, javaPort: Int, serverName: String) {
        targetDir.mkdirs()
        File(targetDir, "config.yml").writeText(
            generateGeyserConfig(
                bedrockPort = bedrockPort,
                remoteJavaPort = javaPort,
                serverName = serverName
            )
        )
    }
}

