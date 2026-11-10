package com.minehost.app

import com.minehost.app.core.common.Difficulty
import com.minehost.app.core.common.GameMode
import com.minehost.app.core.common.MinecraftEdition
import com.minehost.app.server.ServerConfiguration
import com.minehost.app.server.bedrock.BedrockConfigGenerator
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BedrockConfigGeneratorTest {
    private fun config(
        port: Int = 19132,
        maxPlayers: Int = 10,
        viewDistance: Int = 10,
        maxMemoryMb: Int = 1024,
        levelName: String = "world",
        serverName: String = "MineHost Test",
        motd: String = "A MineHost server",
        onlineMode: Boolean = true
    ) = ServerConfiguration(
        serverId = "test-server",
        serverName = serverName,
        motd = motd,
        edition = MinecraftEdition.BEDROCK,
        localPort = port,
        maxPlayers = maxPlayers,
        viewDistance = viewDistance,
        simulationDistance = 4,
        gameMode = GameMode.SURVIVAL,
        difficulty = Difficulty.NORMAL,
        pvpEnabled = true,
        onlineMode = onlineMode,
        levelName = levelName,
        maxMemoryMb = maxMemoryMb
    )

    @Test
    fun emitsPocketMinePropertyNamesAndWildcardListenAddresses() {
        val properties = BedrockConfigGenerator.generateServerProperties(config())
        assertTrue(properties.contains("server-ip=0.0.0.0"))
        assertTrue(properties.contains("server-port=19132"))
        assertTrue(properties.contains("server-ipv6=::"))
        assertTrue(properties.contains("server-portv6=19133"))
        assertTrue(properties.contains("xbox-auth=on"))
        assertFalse(properties.contains("online-mode="))
        assertTrue(properties.contains("enable-query=off"))
    }

    @Test
    fun mapsDisabledXboxAuthenticationToPocketMineProperty() {
        val properties = BedrockConfigGenerator.generateServerProperties(config(onlineMode = false))
        assertTrue(properties.contains("xbox-auth=off"))
    }

    @Test
    fun stripsLineBreaksFromPropertyValues() {
        val properties = BedrockConfigGenerator.generateServerProperties(
            config(serverName = "MineHost\nwhite-list=on", motd = "Hello\r\nmax-players=999")
        )
        assertFalse(properties.contains("\nwhite-list=on"))
        assertFalse(properties.contains("\nmax-players=999"))
    }

    @Test
    fun rejectsInvalidPortsAndMemoryLimits() {
        assertTrue(BedrockConfigGenerator.validate(config(port = 65535)).isFailure)
        assertTrue(BedrockConfigGenerator.validate(config(port = 0)).isFailure)
        assertTrue(BedrockConfigGenerator.validate(config(maxMemoryMb = 128)).isFailure)
    }

    @Test
    fun rejectsUnsafeWorldNamesAndInvalidPlayerSettings() {
        assertTrue(BedrockConfigGenerator.validate(config(levelName = "../outside")).isFailure)
        assertTrue(BedrockConfigGenerator.validate(config(maxPlayers = 0)).isFailure)
        assertTrue(BedrockConfigGenerator.validate(config(viewDistance = 1)).isFailure)
    }

    @Test
    fun writesValidPocketMineMemoryAndWorldSettings() {
        val yaml = BedrockConfigGenerator.generatePocketMineYml(config(maxMemoryMb = 1024))
        assertTrue(yaml.contains("global-limit: 1024M"))
        assertTrue(yaml.contains("main-limit: 896M"))
        assertTrue(yaml.contains("default-world: \"world\""))
    }
}
