package com.kami.gamelist.core.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.kami.gamelist.core.ui.model.GameUi
import com.kami.gamelist.core.ui.model.GridColumnsOption
import com.kami.gamelist.core.ui.theme.LocalGridColumns
import kotlinx.coroutines.delay

@Composable
fun GameGrid(
    games: List<GameUi>,
    onGameClick: (GameUi) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    favoriteIds: Set<Int> = emptySet(),
    onToggleFavorite: ((Int) -> Unit)? = null,
) {
    val gridCells = when (LocalGridColumns.current) {
        GridColumnsOption.ADAPTIVE -> GridCells.Adaptive(minSize = 160.dp)
        GridColumnsOption.TWO -> GridCells.Fixed(2)
        GridColumnsOption.THREE -> GridCells.Fixed(3)
    }

    val gridState = rememberLazyGridState()
    val scrollToTop = LocalScrollToTop.current

    LaunchedEffect(scrollToTop.trigger) {
        if (scrollToTop.trigger > 0) {
            gridState.animateScrollToItem(0)
        }
    }

    LazyVerticalGrid(
        columns = gridCells,
        state = gridState,
        modifier = modifier,
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        itemsIndexed(
            items = games,
            key = { _, game -> game.id }
        ) { index, game ->
            var appeared by remember { mutableStateOf(false) }

            LaunchedEffect(Unit) {
                delay((index % 8) * 40L)
                appeared = true
            }

            val alpha by animateFloatAsState(
                targetValue = if (appeared) 1f else 0f,
                animationSpec = tween(250),
                label = "item_alpha"
            )
            val offsetY by animateFloatAsState(
                targetValue = if (appeared) 0f else 24f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMediumLow
                ),
                label = "item_offset"
            )

            GameCard(
                game = game,
                onClick = { onGameClick(game) },
                isFavorite = game.id in favoriteIds,
                onFavoriteToggle = onToggleFavorite?.let { toggle -> { toggle(game.id) } },
                modifier = Modifier
                    .graphicsLayer {
                        this.alpha = alpha
                        translationY = offsetY
                    }
                    .animateItem()
            )
        }
    }
}

@Composable
fun GameGridSkeleton(
    modifier: Modifier = Modifier,
    itemCount: Int = 6
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 160.dp),
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        userScrollEnabled = false
    ) {
        items(itemCount) {
            GameCardSkeleton()
        }
    }
}
