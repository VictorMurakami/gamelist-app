package com.kami.gamelist.core.di

import com.kami.gamelist.data.repository.GameRepository
import com.kami.gamelist.data.repository.UserRepository
import org.koin.dsl.module

val repositoryModule = module {
    single { GameRepository(get(), get(), get()) }
    single { UserRepository(get()) }
}
