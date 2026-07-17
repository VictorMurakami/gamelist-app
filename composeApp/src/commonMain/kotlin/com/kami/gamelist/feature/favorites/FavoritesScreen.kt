package com.kami.gamelist.feature.favorites

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import com.kami.gamelist.core.ui.components.EmptyState
import com.kami.gamelist.core.ui.components.GameChip
import com.kami.gamelist.core.ui.components.GameGrid
import com.kami.gamelist.core.ui.components.GameToastType
import com.kami.gamelist.core.ui.components.LocalGameToastState
import com.kami.gamelist.core.ui.components.SectionHeader
import com.kami.gamelist.core.ui.theme.GameTheme
import com.kami.gamelist.feature.detail.GameDetailNavScreen

@Composable
fun FavoritesScreen(screenModel: FavoritesScreenModel) {
    val navigator = LocalNavigator.currentOrThrow
    val favorites by screenModel.favorites.collectAsState()
    val favoriteIds by screenModel.favoriteIds.collectAsState()
    val sortOption by screenModel.sortOption.collectAsState()
    val toastState = LocalGameToastState.current
    val colors = GameTheme.colors

    Column(modifier = Modifier.fillMaxSize()) {
        SectionHeader(
            title = if (favorites.isEmpty()) "Favorites" else "Favorites (${favorites.size})"
        )

        if (favorites.isNotEmpty()) {
            Text(
                text = "SORT",
                style = GameTheme.typography.labelSmall,
                color = colors.textMuted,
                modifier = Modifier.padding(start = 16.dp, bottom = 4.dp)
            )
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FavoriteSortOption.entries.forEach { option ->
                    GameChip(
                        label = option.label,
                        selected = option == sortOption,
                        onClick = { screenModel.setSortOption(option) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
        }

        if (favorites.isEmpty()) {
            EmptyState(
                icon = Icons.Outlined.FavoriteBorder,
                title = "No favorites yet",
                subtitle = "Tap the heart on a game to add it here"
            )
        } else {
            GameGrid(
                games = favorites,
                onGameClick = { game -> navigator.push(GameDetailNavScreen(game.id)) },
                favoriteIds = favoriteIds,
                onToggleFavorite = { gameId ->
                    val wasAdded = gameId !in favoriteIds
                    screenModel.toggleFavorite(gameId)
                    if (wasAdded) {
                        toastState.show("Added to favorites", GameToastType.SUCCESS)
                    } else {
                        toastState.show(
                            message = "Removed from favorites",
                            type = GameToastType.INFO,
                            actionLabel = "Undo",
                            onAction = { screenModel.toggleFavorite(gameId) }
                        )
                    }
                }
            )
        }
    }
}
