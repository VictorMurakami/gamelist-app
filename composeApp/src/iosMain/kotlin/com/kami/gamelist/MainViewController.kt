package com.kami.gamelist

import androidx.compose.ui.window.ComposeUIViewController
import com.kami.gamelist.core.database.DriverFactory
import com.kami.gamelist.core.di.appModules
import com.kami.gamelist.core.network.ConnectivityMonitor
import com.kami.gamelist.core.platform.UrlOpener
import org.koin.core.context.startKoin
import org.koin.dsl.module

fun initKoin() {
    startKoin {
        modules(appModules())
        modules(module {
            single { DriverFactory() }
            single { ConnectivityMonitor() }
            single { UrlOpener() }
        })
    }
}

fun MainViewController() = ComposeUIViewController {
    App()
}
