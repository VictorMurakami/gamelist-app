package com.kami.gamelist.core.di

import com.kami.gamelist.core.config.AppConfigApi
import com.kami.gamelist.core.network.HttpClientFactory
import com.kami.gamelist.data.remote.FreeToGameApi
import org.koin.core.qualifier.named
import org.koin.dsl.module

val networkModule = module {
    single { HttpClientFactory.create() }
    single { FreeToGameApi(get()) }

    single(named("backend")) { HttpClientFactory.createBackend() }
    single { AppConfigApi(get(named("backend"))) }
}
