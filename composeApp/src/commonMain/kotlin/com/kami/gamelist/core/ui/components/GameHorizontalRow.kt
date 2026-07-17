package com.kami.gamelist.core.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kami.gamelist.core.ui.model.GameUi
import com.kami.gamelist.core.ui.theme.GameTheme

@Composable
fun GameHorizontalRow(
    title: String,
    games: List<GameUi>,
    onGameClick: (GameUi) -> Unit,
    onSeeAllClick: () -> Unit,
    modifier: Modifier = Modifier,
    favoriteIds: Set<Int> = emptySet(),
    onToggleFavorite: ((Int) -> Unit)? = null,
) {
    val colors = GameTheme.colors

    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = GameTheme.typography.titleMedium,
                color = colors.textPrimary
            )
            Text(
                text = "See All",
                style = GameTheme.typography.labelSmall,
                color = colors.accent,
                modifier = Modifier
                    .clickable(onClick = onSeeAllClick)
                    .padding(vertical = 4.dp, horizontal = 4.dp)
            )
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(games, key = { it.id }) { game ->
                GameCard(
                    game = game,
                    onClick = { onGameClick(game) },
                    isFavorite = game.id in favoriteIds,
                    onFavoriteToggle = onToggleFavorite?.let { toggle -> { toggle(game.id) } },
                    modifier = Modifier.width(160.dp)
                )
            }
        }
    }
}
