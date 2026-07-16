package com.kami.gamelist.core.di

import com.kami.gamelist.feature.detail.GameDetailScreenModel
import com.kami.gamelist.feature.favorites.FavoritesScreenModel
import com.kami.gamelist.feature.search.SearchScreenModel
import com.kami.gamelist.core.network.ConnectivityMonitor
import com.kami.gamelist.feature.home.HomeScreenModel
import org.koin.dsl.module

val featureModule = module {
    // Task 9 - Home Feature
    factory { HomeScreenModel(get(), get<ConnectivityMonitor>().isOnline) }

    // Task 10 - Search Feature
    factory { SearchScreenModel(get(), get()) }

    // Task 11 - Game Detail Feature
    factory { params -> GameDetailScreenModel(params.get(), get(), get()) }

    // Task 12 - Favorites Feature
    factory { FavoritesScreenModel(get()) }

    // TODO: Task 13 - Lists Feature
    // factory { ListsScreenModel(get()) }
    // factory { params -> ListDetailScreenModel(params.get(), get()) }
}
