package com.kami.gamelist.feature.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.List
import androidx.compose.material.icons.outlined.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions
import com.kami.gamelist.feature.home.HomeScreen
import com.kami.gamelist.feature.home.HomeScreenModel
import com.kami.gamelist.feature.search.SearchScreen
import com.kami.gamelist.feature.search.SearchScreenModel
import com.kami.gamelist.feature.favorites.FavoritesScreen
import com.kami.gamelist.feature.lists.ListsScreen

object HomeTab : Tab {
    override val options: TabOptions
        @Composable get() {
            val icon = rememberVectorPainter(Icons.Outlined.Home)
            return remember { TabOptions(index = 0u, title = "Home", icon = icon) }
        }

    @Composable
    override fun Content() {
        val screenModel = koinScreenModel<HomeScreenModel>()
        HomeScreen(screenModel)
    }
}

object SearchTab : Tab {
    override val options: TabOptions
        @Composable get() {
            val icon = rememberVectorPainter(Icons.Outlined.Search)
            return remember { TabOptions(index = 1u, title = "Search", icon = icon) }
        }

    @Composable
    override fun Content() {
        val screenModel = koinScreenModel<SearchScreenModel>()
        SearchScreen(screenModel)
    }
}

object FavoritesTab : Tab {
    override val options: TabOptions
        @Composable get() {
            val icon = rememberVectorPainter(Icons.Outlined.FavoriteBorder)
            return remember { TabOptions(index = 2u, title = "Favorites", icon = icon) }
        }

    @Composable
    override fun Content() {
        FavoritesScreen()
    }
}

object ListsTab : Tab {
    override val options: TabOptions
        @Composable get() {
            val icon = rememberVectorPainter(Icons.Outlined.List)
            return remember { TabOptions(index = 3u, title = "Lists", icon = icon) }
        }

    @Composable
    override fun Content() {
        ListsScreen()
    }
}
