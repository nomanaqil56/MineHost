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

    /**
     * Returns the engine for an edition, or null if the edition has no working engine.
     *
     * CROSSPLAY intentionally returns null: genuine cross-play needs Paper + Geyser running
     * together, which is not implemented (see docs/KNOWN_ISSUES.md). It must never silently
     * fall back to a Bedrock-only server.
     */
    fun getEngineForEdition(edition: MinecraftEdition): ServerEngine? {
        return when (edition) {
            MinecraftEdition.BEDROCK -> bedrockEngine
            MinecraftEdition.JAVA -> javaEngine
            MinecraftEdition.CROSSPLAY -> null
        }
    }

    fun getAllEngines(): List<ServerEngine> = engines.values.toList()
}

