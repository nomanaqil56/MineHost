package com.minehost.app.data.local

import androidx.room.TypeConverter
import com.minehost.app.core.common.Difficulty
import com.minehost.app.core.common.GameMode
import com.minehost.app.core.common.LogLevel
import com.minehost.app.core.common.MinecraftEdition
import com.minehost.app.core.common.ServerStatus

class DatabaseConverters {

    @TypeConverter
    fun fromServerStatus(status: ServerStatus): String = status.name

    @TypeConverter
    fun toServerStatus(value: String): ServerStatus = runCatching {
        ServerStatus.valueOf(value)
    }.getOrDefault(ServerStatus.STOPPED)

    @TypeConverter
    fun fromMinecraftEdition(edition: MinecraftEdition): String = edition.name

    @TypeConverter
    fun toMinecraftEdition(value: String): MinecraftEdition = runCatching {
        MinecraftEdition.valueOf(value)
    }.getOrDefault(MinecraftEdition.BEDROCK)

    @TypeConverter
    fun fromGameMode(mode: GameMode): String = mode.name

    @TypeConverter
    fun toGameMode(value: String): GameMode = runCatching {
        GameMode.valueOf(value)
    }.getOrDefault(GameMode.SURVIVAL)

    @TypeConverter
    fun fromDifficulty(diff: Difficulty): String = diff.name

    @TypeConverter
    fun toDifficulty(value: String): Difficulty = runCatching {
        Difficulty.valueOf(value)
    }.getOrDefault(Difficulty.NORMAL)

    @TypeConverter
    fun fromLogLevel(level: LogLevel): String = level.name

    @TypeConverter
    fun toLogLevel(value: String): LogLevel = runCatching {
        LogLevel.valueOf(value)
    }.getOrDefault(LogLevel.INFO)
}
