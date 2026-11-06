package com.minehost.app.server

import com.minehost.app.core.common.MinecraftEdition
import com.minehost.app.server.bedrock.BedrockEngine
import com.minehost.app.server.java.JavaEngine
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ServerEngineRegistry @Inject constructor(
    private val bedrockEngine: BedrockEngine,
    private val javaEngine: JavaEngine
) {
    private val engines = mapOf(
        bedrockEngine.id to bedrockEngine,
        javaEngine.id to javaEngine
    )

    fun getEngine(id: String): ServerEngine? = engines[id]

    fun getEngineForEdition(edition: MinecraftEdition): ServerEngine {
        return when (edition) {
            MinecraftEdition.BEDROCK -> bedrockEngine
            MinecraftEdition.JAVA -> javaEngine
            MinecraftEdition.CROSSPLAY -> bedrockEngine // Bridges to Java
        }
    }

    fun getAllEngines(): List<ServerEngine> = engines.values.toList()
}

