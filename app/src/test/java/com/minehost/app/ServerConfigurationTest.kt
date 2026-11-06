package com.minehost.app

import com.minehost.app.core.common.Difficulty
import com.minehost.app.core.common.GameMode
import com.minehost.app.core.common.MinecraftEdition
import com.minehost.app.server.ServerConfiguration
import com.minehost.app.server.bedrock.BedrockConfigGenerator
import com.minehost.app.server.geyser.GeyserConfigGenerator
import com.minehost.app.server.java.JavaConfigGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ServerConfigurationTest {

    @Test
    fun testBedrockServerPropertiesGeneration() {
        val config = ServerConfiguration(
            serverId = "srv_test_01",
            serverName = "Alpha Realm",
            motd = "Welcome to Alpha Realm",
            edition = MinecraftEdition.BEDROCK,
            localPort = 19132,
            maxPlayers = 16,
            viewDistance = 12,
            simulationDistance = 6,
            gameMode = GameMode.SURVIVAL,
            difficulty = Difficulty.HARD,
            pvpEnabled = true,
            onlineMode = true
        )

        val properties = BedrockConfigGenerator.generateServerProperties(config)

        assertTrue(properties.contains("server-name=Alpha Realm"))
        assertTrue(properties.contains("server-port=19132"))
        assertTrue(properties.contains("max-players=16"))
        assertTrue(properties.contains("gamemode=0")) // Survival is 0
        assertTrue(properties.contains("difficulty=3")) // Hard is 3
        assertTrue(properties.contains("pvp=on"))
        assertTrue(properties.contains("online-mode=on"))
    }

    @Test
    fun testJavaServerPropertiesGeneration() {
        val config = ServerConfiguration(
            serverId = "srv_java_01",
            serverName = "Paper Server",
            motd = "Java Minecraft on Android",
            edition = MinecraftEdition.JAVA,
            localPort = 25565,
            maxPlayers = 20,
            viewDistance = 10,
            simulationDistance = 4,
            gameMode = GameMode.CREATIVE,
            difficulty = Difficulty.PEACEFUL,
            pvpEnabled = false,
            onlineMode = false
        )

        val properties = JavaConfigGenerator.generateServerProperties(config)

        assertTrue(properties.contains("server-port=25565"))
        assertTrue(properties.contains("max-players=20"))
        assertTrue(properties.contains("gamemode=creative"))
        assertTrue(properties.contains("difficulty=peaceful"))
        assertTrue(properties.contains("pvp=false"))
        assertTrue(properties.contains("online-mode=false"))

        val eula = JavaConfigGenerator.generateEula()
        assertTrue(eula.contains("eula=true"))
    }

    @Test
    fun testGeyserCrossPlayConfigGeneration() {
        val config = GeyserConfigGenerator.generateGeyserConfig(
            bedrockPort = 19132,
            remoteJavaPort = 25565,
            serverName = "CrossPlay Hub"
        )

        assertTrue(config.contains("port: 19132"))
        assertTrue(config.contains("port: 25565"))
        assertTrue(config.contains("address: 127.0.0.1"))
        assertTrue(config.contains("auth-type: floodgate"))
    }
}
