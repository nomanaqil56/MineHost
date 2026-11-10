package com.minehost.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.minehost.app.data.local.dao.BackupDao
import com.minehost.app.data.local.dao.PlayerDao
import com.minehost.app.data.local.dao.ServerDao
import com.minehost.app.data.local.dao.ServerLogDao
import com.minehost.app.data.local.dao.WorldDao
import com.minehost.app.data.local.entity.BackupEntity
import com.minehost.app.data.local.entity.PlayerEntity
import com.minehost.app.data.local.entity.ServerEntity
import com.minehost.app.data.local.entity.ServerLogEntity
import com.minehost.app.data.local.entity.WorldEntity

@Database(
    entities = [
        ServerEntity::class,
        WorldEntity::class,
        BackupEntity::class,
        PlayerEntity::class,
        ServerLogEntity::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(DatabaseConverters::class)
abstract class MineHostDatabase : RoomDatabase() {
    abstract fun serverDao(): ServerDao
    abstract fun worldDao(): WorldDao
    abstract fun backupDao(): BackupDao
    abstract fun playerDao(): PlayerDao
    abstract fun serverLogDao(): ServerLogDao
}


/**
 * v1 -> v2: persist Minecraft EULA consent per server.
 * Existing servers get eulaAccepted = 0 (not accepted); no user data is removed.
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE servers ADD COLUMN eulaAccepted INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE servers ADD COLUMN eulaAcceptedAt INTEGER")
    }
}
