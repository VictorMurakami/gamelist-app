package com.kami.gamelist.core.di

import com.kami.gamelist.core.config.DeviceIdProvider
import com.kami.gamelist.core.database.DriverFactory
import com.kami.gamelist.data.local.CacheManager
import com.kami.gamelist.data.local.GameLocalDataSource
import com.kami.gamelist.data.local.TextPrefDataSource
import com.kami.gamelist.data.local.UserLocalDataSource
import com.kami.gamelist.db.GameListDatabase
import org.koin.dsl.module

val databaseModule = module {
    single { get<DriverFactory>().createDriver() }
    single { GameListDatabase(get()) }
    single { GameLocalDataSource(get()) }
    single { UserLocalDataSource(get()) }
    single { CacheManager(get()) }
    single { TextPrefDataSource(get()) }
    single { DeviceIdProvider(get()) }
}
