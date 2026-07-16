package com.kami.gamelist.feature.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.outlined.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions
import com.kami.gamelist.feature.home.HomeScreen
import com.kami.gamelist.feature.home.HomeScreenModel
import com.kami.gamelist.feature.search.SearchScreen
import com.kami.gamelist.feature.search.SearchScreenModel
import com.kami.gamelist.feature.favorites.FavoritesScreen
import com.kami.gamelist.feature.lists.ListsScreen

private class HomeWrapperScreen : Screen {
    @Composable
    override fun Content() {
        val screenModel = koinScreenModel<HomeScreenModel>()
        HomeScreen(screenModel)
    }
}

private class SearchWrapperScreen : Screen {
    @Composable
    override fun Content() {
        val screenModel = koinScreenModel<SearchScreenModel>()
        SearchScreen(screenModel)
    }
}

private class FavoritesWrapperScreen : Screen {
    @Composable
    override fun Content() {
        FavoritesScreen()
    }
}

private class ListsWrapperScreen : Screen {
    @Composable
    override fun Content() {
        ListsScreen()
    }
}

object HomeTab : Tab {
    override val options: TabOptions
        @Composable get() {
            val icon = rememberVectorPainter(Icons.Outlined.Home)
            return remember { TabOptions(index = 0u, title = "Home", icon = icon) }
        }

    @Composable
    override fun Content() {
        Navigator(HomeWrapperScreen())
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
        Navigator(SearchWrapperScreen())
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
        Navigator(FavoritesWrapperScreen())
    }
}

object ListsTab : Tab {
    override val options: TabOptions
        @Composable get() {
            val icon = rememberVectorPainter(Icons.AutoMirrored.Outlined.List)
            return remember { TabOptions(index = 3u, title = "Lists", icon = icon) }
        }

    @Composable
    override fun Content() {
        Navigator(ListsWrapperScreen())
    }
}
