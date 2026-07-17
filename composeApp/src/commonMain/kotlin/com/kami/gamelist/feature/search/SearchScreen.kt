package com.kami.gamelist.feature.search

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.kami.gamelist.core.ui.components.EmptyState
import com.kami.gamelist.core.ui.components.GameGrid
import com.kami.gamelist.core.ui.components.GameSearchBar
import com.kami.gamelist.core.ui.components.GameToastType
import com.kami.gamelist.core.ui.components.LocalGameToastState
import com.kami.gamelist.feature.detail.GameDetailNavScreen

@Composable
fun SearchScreen(screenModel: SearchScreenModel) {
    val navigator = LocalNavigator.currentOrThrow
    val query by screenModel.query.collectAsState()
    val results by screenModel.searchResults.collectAsState()
    val recentSearches by screenModel.recentSearches.collectAsState()
    val showHistory by screenModel.showHistory.collectAsState()
    val favoriteIds by screenModel.favoriteIds.collectAsState()
    val toastState = LocalGameToastState.current

    Column(modifier = Modifier.fillMaxSize()) {
        Spacer(modifier = Modifier.height(16.dp))

        GameSearchBar(
            query = query,
            onQueryChange = screenModel::onQueryChange,
            onSearch = screenModel::onSearch,
            recentSearches = recentSearches,
            showHistory = showHistory,
            onHistoryItemClick = screenModel::onHistoryItemClick,
            onHistoryItemDelete = screenModel::onHistoryItemDelete
        )

        Spacer(modifier = Modifier.height(8.dp))

        when {
            query.isBlank() && recentSearches.isEmpty() && showHistory -> {
                EmptyState(
                    icon = Icons.Outlined.SportsEsports,
                    title = "Search for games",
                    subtitle = "Find free-to-play games by name"
                )
            }

            query.isNotBlank() && results.isEmpty() -> {
                EmptyState(
                    icon = Icons.Outlined.Search,
                    title = "No results",
                    subtitle = "Try a different search term"
                )
            }

            results.isNotEmpty() -> {
                GameGrid(
                    games = results,
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
