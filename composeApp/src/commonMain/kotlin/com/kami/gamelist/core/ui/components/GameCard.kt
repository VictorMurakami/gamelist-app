package com.kami.gamelist.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import com.kami.gamelist.core.ui.model.GameUi
import com.kami.gamelist.core.ui.modifier.pressScale
import com.kami.gamelist.core.ui.theme.GameTheme

@Composable
fun GameCard(
    game: GameUi,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isFavorite: Boolean = false,
    onFavoriteToggle: (() -> Unit)? = null,
) {
    val colors = GameTheme.colors

    GameSurface(
        modifier = modifier.pressScale(onClick = onClick),
        backgroundColor = colors.surfaceElevated,
        borderColor = colors.borderSubtle,
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

        Column(modifier = Modifier.padding(8.dp)) {
            Text(
                text = game.title,
                style = GameTheme.typography.titleSmall,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

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
}

@Composable
fun GameCardSkeleton(modifier: Modifier = Modifier) {
    val colors = GameTheme.colors

    GameSurface(
        modifier = modifier,
        backgroundColor = colors.surfaceElevated,
        borderColor = colors.borderSubtle,
        cornerRadius = 6.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .shimmerEffect()
        )

        Column(modifier = Modifier.padding(8.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(16.dp)
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(2.dp))
                    .shimmerEffect()
            )
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.4f)
                    .height(12.dp)
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(2.dp))
                    .shimmerEffect()
            )
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.5f)
                    .height(12.dp)
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(2.dp))
                    .shimmerEffect()
            )
        }
    }
}
