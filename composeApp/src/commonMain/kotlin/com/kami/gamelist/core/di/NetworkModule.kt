package com.kami.gamelist.core.di

import com.kami.gamelist.core.network.HttpClientFactory
import com.kami.gamelist.data.remote.FreeToGameApi
import org.koin.dsl.module

val networkModule = module {
    single { HttpClientFactory.create() }
    single { FreeToGameApi(get()) }
}
