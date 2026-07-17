package com.kami.gamelist.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.SubcomposeAsyncImage
import com.kami.gamelist.core.ui.model.GameUi
import com.kami.gamelist.core.ui.modifier.pressScale
import com.kami.gamelist.core.ui.theme.GameTheme

@Composable
fun GameRankedRow(
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
            itemsIndexed(games, key = { _, game -> game.id }) { index, game ->
                RankedCard(
                    rank = index + 1,
                    game = game,
                    onClick = { onGameClick(game) },
                    isFavorite = game.id in favoriteIds,
                    onFavoriteToggle = onToggleFavorite?.let { toggle -> { toggle(game.id) } },
                )
            }
        }
    }
}

@Composable
private fun RankedCard(
    rank: Int,
    game: GameUi,
    onClick: () -> Unit,
    isFavorite: Boolean = false,
    onFavoriteToggle: (() -> Unit)? = null,
) {
    val colors = GameTheme.colors
    val rankColor = when (rank) {
        1 -> Color(0xFFFFD700)
        2 -> Color(0xFFC0C0C0)
        3 -> Color(0xFFCD7F32)
        else -> colors.textMuted
    }

    GameSurface(
        modifier = Modifier
            .width(150.dp)
            .pressScale(onClick = onClick),
        backgroundColor = colors.surfaceElevated,
        borderColor = if (rank <= 3) rankColor.copy(alpha = 0.3f) else colors.borderSubtle,
        cornerRadius = 6.dp
    ) {
        Box {
            SubcomposeAsyncImage(
                model = game.thumbnail,
                contentDescription = game.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f),
                loading = {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .shimmerEffect()
                    )
                }
            )

            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(6.dp)
                    .size(28.dp)
                    .background(
                        if (rank <= 3) rankColor else colors.surfaceOverlay,
                        RoundedCornerShape(4.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$rank",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (rank <= 3) Color(0xFF1A1A2E) else colors.textPrimary,
                )
            }

            if (onFavoriteToggle != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .background(
                            colors.backgroundDark.copy(alpha = 0.6f),
                            CircleShape
                        )
                ) {
                    FavoriteButton(
                        isFavorite = isFavorite,
                        onToggle = onFavoriteToggle,
                    )
                }
            }
        }

        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = game.title,
                    style = GameTheme.typography.titleSmall,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = game.genre,
                    style = GameTheme.typography.labelSmall,
                    color = colors.accent
                )
            }
        }
    }
}
