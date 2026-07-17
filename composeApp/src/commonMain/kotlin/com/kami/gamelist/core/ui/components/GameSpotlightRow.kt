package com.kami.gamelist.core.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import com.kami.gamelist.core.ui.model.GameUi
import com.kami.gamelist.core.ui.modifier.pressScale
import com.kami.gamelist.core.ui.theme.GameTheme
import kotlinx.coroutines.delay
import kotlin.math.absoluteValue

@Composable
fun GameSpotlightRow(
    title: String,
    games: List<GameUi>,
    onGameClick: (GameUi) -> Unit,
    onSeeAllClick: () -> Unit,
    modifier: Modifier = Modifier,
    favoriteIds: Set<Int> = emptySet(),
    onToggleFavorite: ((Int) -> Unit)? = null,
) {
    val colors = GameTheme.colors
    if (games.isEmpty()) return

    val displayGames = games.take(8)
    val pagerState = rememberPagerState(pageCount = { displayGames.size })

    LaunchedEffect(pagerState) {
        while (true) {
            delay(4000)
            val pageCount = pagerState.pageCount
            if (pageCount > 1) {
                val next = (pagerState.currentPage + 1) % pageCount
                pagerState.animateScrollToPage(next)
            }
        }
    }

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

        Spacer(modifier = Modifier.height(8.dp))

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth(),
            pageSpacing = 12.dp,
            beyondViewportPageCount = 1
        ) { page ->
            val game = displayGames[page]
            val pageOffset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction).absoluteValue

            SpotlightCard(
                game = game,
                isFavorite = game.id in favoriteIds,
                onClick = { onGameClick(game) },
                onFavoriteToggle = onToggleFavorite?.let { toggle -> { toggle(game.id) } },
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .graphicsLayer {
                        val scale = 1f - (pageOffset * 0.05f).coerceIn(0f, 0.05f)
                        scaleX = scale
                        scaleY = scale
                        alpha = 1f - (pageOffset * 0.3f).coerceIn(0f, 0.3f)
                    }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(displayGames.size) { index ->
                val isSelected = pagerState.currentPage == index
                val dotSize by animateDpAsState(
                    targetValue = if (isSelected) 8.dp else 5.dp,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                )
                Box(
                    modifier = Modifier
                        .padding(horizontal = 3.dp)
                        .size(dotSize)
                        .clip(CircleShape)
                        .background(
                            if (isSelected) colors.accent else colors.textMuted.copy(alpha = 0.4f)
                        )
                )
            }
        }
    }
}

@Composable
private fun SpotlightCard(
    game: GameUi,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onFavoriteToggle: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val colors = GameTheme.colors

    GameSurface(
        modifier = modifier.pressScale(onClick = onClick),
        backgroundColor = colors.surfaceElevated,
        borderColor = colors.borderSubtle,
        cornerRadius = 8.dp
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
                    .fillMaxWidth()
                    .height(72.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                colors.surfaceElevated.copy(alpha = 0.95f)
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp)
            ) {
                Text(
                    text = game.title,
                    style = GameTheme.typography.titleMedium,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = game.genre,
                        style = GameTheme.typography.labelSmall,
                        color = colors.accent
                    )
                    Text(
                        text = game.platform,
                        style = GameTheme.typography.labelSmall,
                        color = colors.textMuted
                    )
                }
            }

            if (onFavoriteToggle != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
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
    }
}
