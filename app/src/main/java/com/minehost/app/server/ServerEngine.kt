package com.minehost.app.server

import com.minehost.app.core.common.Difficulty
import com.minehost.app.core.common.GameMode
import com.minehost.app.core.common.MinecraftEdition
import com.minehost.app.core.common.ServerStatus
import kotlinx.coroutines.flow.StateFlow
import java.io.File

data class ServerConfiguration(
    val serverId: String,
    val serverName: String,
    val motd: String,
    val edition: MinecraftEdition,
    val localPort: Int,
    val maxPlayers: Int,
    val viewDistance: Int,
    val simulationDistance: Int,
    val gameMode: GameMode,
    val difficulty: Difficulty,
    val pvpEnabled: Boolean,
    val onlineMode: Boolean,
    val allowFlight: Boolean = true,
    val spawnProtection: Int = 16,
    val enableCommandBlocks: Boolean = true,
    val levelName: String = "world",
    val minMemoryMb: Int = 256,
    val maxMemoryMb: Int = 1024,
    val customJvmArgs: List<String> = emptyList(),
    /** True only after the user explicitly accepted the Minecraft EULA in the app. */
    val eulaAccepted: Boolean = false
)

interface ProcessListener {
    fun onLog(line: String)
    fun onStarted()
    fun onStopped(exitCode: Int)
    fun onError(error: String)
}

interface ServerEngine {
    val id: String
    val name: String
    val edition: MinecraftEdition
    val defaultVersion: String
    val supportedVersions: List<String>

    fun isInstalled(): Boolean
    suspend fun install(onProgress: (Float, String) -> Unit): Result<Unit>
    suspend fun configure(serverDir: File, config: ServerConfiguration): Result<Unit>
    suspend fun start(serverDir: File, config: ServerConfiguration, listener: ProcessListener): Result<ServerProcessHandle>
    suspend fun stop(handle: ServerProcessHandle, timeoutMs: Long = 15_000L): Boolean
    fun sendCommand(handle: ServerProcessHandle, command: String): Boolean
}

interface ServerProcessHandle {
    val pid: Long
    val isAlive: Boolean
    fun destroy()
    fun destroyForcibly()
}

