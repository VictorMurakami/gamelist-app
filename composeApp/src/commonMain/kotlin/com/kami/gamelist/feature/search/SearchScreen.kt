package com.kami.gamelist.feature.search

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.kami.gamelist.core.ui.components.EmptyState
import com.kami.gamelist.feature.detail.GameDetailNavScreen
import com.kami.gamelist.core.ui.components.GameGrid
import com.kami.gamelist.core.ui.components.GameSearchBar

@Composable
fun SearchScreen(screenModel: SearchScreenModel) {
    val navigator = LocalNavigator.currentOrThrow
    val query by screenModel.query.collectAsState()
    val results by screenModel.searchResults.collectAsState()
    val recentSearches by screenModel.recentSearches.collectAsState()
    val showHistory by screenModel.showHistory.collectAsState()

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

        if (query.isNotBlank() && results.isEmpty()) {
            EmptyState(
                icon = Icons.Outlined.Search,
                title = "No results",
                subtitle = "Try a different search term"
            )
        } else if (results.isNotEmpty()) {
            GameGrid(
                games = results,
                onGameClick = { game -> navigator.push(GameDetailNavScreen(game.id)) }
            )
        }
    }
}
