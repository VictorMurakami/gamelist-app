package com.kami.gamelist

import android.app.Application
import com.kami.gamelist.core.database.DriverFactory
import com.kami.gamelist.core.di.appModules
import com.kami.gamelist.core.network.ConnectivityMonitor
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.dsl.module

class GameListApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@GameListApplication)
            modules(appModules())
            modules(module {
                single { DriverFactory(get()) }
                single { ConnectivityMonitor(get()) }
            })
        }
    }
}
