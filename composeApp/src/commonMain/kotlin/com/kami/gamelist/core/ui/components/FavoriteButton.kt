package com.kami.gamelist.core.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.kami.gamelist.core.ui.theme.GameTheme

@Composable
fun FavoriteButton(
    isFavorite: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = GameTheme.colors
    var wasClicked by remember { mutableStateOf(false) }

    val clickScale by animateFloatAsState(
        targetValue = if (wasClicked) 1.4f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        finishedListener = { wasClicked = false },
        label = "fav_scale"
    )

    val clickRotation by animateFloatAsState(
        targetValue = if (wasClicked) -15f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "fav_rotation"
    )

    val animatedTint by animateColorAsState(
        targetValue = if (isFavorite) colors.neonPink else colors.textMuted,
        animationSpec = tween(300),
        label = "fav_color"
    )

    Box(
        modifier = modifier
            .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                wasClicked = true
                onToggle()
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
            contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
            tint = animatedTint,
            modifier = Modifier
                .size(28.dp)
                .graphicsLayer {
                    scaleX = clickScale
                    scaleY = clickScale
                    rotationZ = clickRotation
                }
        )
    }
}
