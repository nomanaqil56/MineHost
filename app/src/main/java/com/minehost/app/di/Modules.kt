package com.minehost.app.di

import android.content.Context
import androidx.room.Room
import com.minehost.app.core.common.Constants
import com.minehost.app.data.local.MineHostDatabase
import com.minehost.app.data.local.dao.BackupDao
import com.minehost.app.data.local.dao.PlayerDao
import com.minehost.app.data.local.dao.ServerDao
import com.minehost.app.data.local.dao.ServerLogDao
import com.minehost.app.data.local.dao.WorldDao
import com.minehost.app.data.repositories.BackupRepositoryImpl
import com.minehost.app.data.repositories.LogRepositoryImpl
import com.minehost.app.data.repositories.PlayerRepositoryImpl
import com.minehost.app.data.repositories.ServerRepositoryImpl
import com.minehost.app.data.repositories.WorldRepositoryImpl
import com.minehost.app.domain.repository.BackupRepository
import com.minehost.app.domain.repository.LogRepository
import com.minehost.app.domain.repository.PlayerRepository
import com.minehost.app.domain.repository.ServerRepository
import com.minehost.app.domain.repository.WorldRepository
import com.minehost.app.networking.UnavailableTunnelProvider
import com.minehost.app.networking.TunnelProvider
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): MineHostDatabase {
        return Room.databaseBuilder(
            context,
            MineHostDatabase::class.java,
            Constants.DATABASE_NAME
        )
            .addMigrations(MIGRATION_1_2)
            .build()
    }

    @Provides
    fun provideServerDao(db: MineHostDatabase): ServerDao = db.serverDao()

    @Provides
    fun provideWorldDao(db: MineHostDatabase): WorldDao = db.worldDao()

    @Provides
    fun provideBackupDao(db: MineHostDatabase): BackupDao = db.backupDao()

    @Provides
    fun providePlayerDao(db: MineHostDatabase): PlayerDao = db.playerDao()

    @Provides
    fun provideServerLogDao(db: MineHostDatabase): ServerLogDao = db.serverLogDao()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindServerRepository(impl: ServerRepositoryImpl): ServerRepository

    @Binds
    @Singleton
    abstract fun bindWorldRepository(impl: WorldRepositoryImpl): WorldRepository

    @Binds
    @Singleton
    abstract fun bindBackupRepository(impl: BackupRepositoryImpl): BackupRepository

    @Binds
    @Singleton
    abstract fun bindPlayerRepository(impl: PlayerRepositoryImpl): PlayerRepository

    @Binds
    @Singleton
    abstract fun bindLogRepository(impl: LogRepositoryImpl): LogRepository

    @Binds
    @Singleton
    abstract fun bindTunnelProvider(impl: UnavailableTunnelProvider): TunnelProvider
}

