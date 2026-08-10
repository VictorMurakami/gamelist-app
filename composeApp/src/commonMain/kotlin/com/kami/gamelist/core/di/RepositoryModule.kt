package com.kami.gamelist.core.di

import com.kami.gamelist.core.config.AppConfigRepository
import com.kami.gamelist.core.config.currentAppInfo
import com.kami.gamelist.data.repository.GameRepository
import com.kami.gamelist.data.repository.UserRepository
import org.koin.dsl.module

val repositoryModule = module {
    single { GameRepository(get(), get(), get()) }
    single { UserRepository(get()) }
    single { currentAppInfo() }
    single { AppConfigRepository(get(), get(), get(), get()) }
}
