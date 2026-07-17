package com.kami.gamelist.feature.lists

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import com.kami.gamelist.core.ui.components.EmptyState
import com.kami.gamelist.core.ui.components.GameGrid
import com.kami.gamelist.core.ui.components.GameToastType
import com.kami.gamelist.core.ui.components.LocalGameToastState
import com.kami.gamelist.core.ui.theme.GameTheme
import com.kami.gamelist.feature.detail.GameDetailNavScreen
import com.kami.gamelist.feature.navigation.HomeTab
import org.koin.core.parameter.parametersOf

data class ListDetailNavScreen(val listId: Long, val listName: String) : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = koinScreenModel<ListDetailScreenModel> { parametersOf(listId) }
        val games by screenModel.games.collectAsState()
        val favoriteIds by screenModel.favoriteIds.collectAsState()
        val toastState = LocalGameToastState.current
        val tabNavigator = LocalTabNavigator.current
        val colors = GameTheme.colors

        Scaffold(
            containerColor = colors.backgroundDark,
            contentWindowInsets = WindowInsets(0),
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = listName.uppercase(),
                            style = GameTheme.typography.headlineSmall,
                            color = colors.textPrimary
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Navigate back",
                                tint = colors.textPrimary
                            )
                        }
                    },
                    windowInsets = WindowInsets(0),
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = colors.backgroundDark
                    )
                )
            }
        ) { padding ->
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                if (games.isEmpty()) {
                    EmptyState(
                        icon = Icons.AutoMirrored.Outlined.List,
                        title = "No games yet",
                        subtitle = "Browse the catalog and tap the list icon on a game's detail screen to add it here",
                        actionLabel = "Browse Games",
                        onAction = {
                            navigator.pop()
                            tabNavigator.current = HomeTab
                        }
                    )
                } else {
                    GameGrid(
                        games = games,
                        onGameClick = { game -> navigator.push(GameDetailNavScreen(game.id)) },
                        favoriteIds = favoriteIds,
                        onToggleFavorite = { gameId ->
                            val wasAdded = gameId !in favoriteIds
                            screenModel.toggleFavorite(gameId)
                            toastState.show(
                                if (wasAdded) "Added to favorites" else "Removed from favorites",
                                if (wasAdded) GameToastType.SUCCESS else GameToastType.INFO
                            )
                        }
                    )
                }
            }
        }
    }
}
