package com.kami.gamelist.core.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kami.gamelist.core.ui.theme.GameTheme
import kotlinx.coroutines.delay

enum class GameToastType { SUCCESS, ERROR, INFO }

data class GameToastData(
    val message: String,
    val type: GameToastType,
    val id: Long,
    val actionLabel: String? = null,
    val onAction: (() -> Unit)? = null,
)

@Stable
class GameToastState {
    var current by mutableStateOf<GameToastData?>(null)
        private set

    private var nextId = 0L

    fun show(
        message: String,
        type: GameToastType = GameToastType.INFO,
        actionLabel: String? = null,
        onAction: (() -> Unit)? = null,
    ) {
        current = GameToastData(message, type, nextId++, actionLabel, onAction)
    }

    fun dismiss() {
        current = null
    }
}

@Composable
fun rememberGameToastState(): GameToastState = remember { GameToastState() }

val LocalGameToastState = staticCompositionLocalOf<GameToastState> {
    error("No GameToastState provided")
}

@Composable
fun GameToastHost(
    state: GameToastState,
    modifier: Modifier = Modifier
) {
    val data = state.current
    var displayedData by remember { mutableStateOf<GameToastData?>(null) }

    LaunchedEffect(data) {
        if (data != null) {
            displayedData = data
            delay(3000)
            state.dismiss()
        }
    }

    AnimatedVisibility(
        visible = data != null,
        enter = fadeIn(tween(200)) + slideInVertically(tween(250)) { it / 2 },
        exit = fadeOut(tween(150)) + slideOutVertically(tween(200)) { it / 2 },
        modifier = modifier
    ) {
        val toastData = displayedData ?: return@AnimatedVisibility
        val colors = GameTheme.colors

        val accentColor = when (toastData.type) {
            GameToastType.SUCCESS -> colors.neonGreen
            GameToastType.ERROR -> colors.error
            GameToastType.INFO -> colors.accent
        }

        GameSurface(
            backgroundColor = colors.surfaceElevated,
            borderColor = accentColor.copy(alpha = 0.5f),
            cornerRadius = 8.dp,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 4.dp, height = 24.dp)
                        .background(accentColor, RoundedCornerShape(2.dp))
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = toastData.message,
                    style = GameTheme.typography.bodyMedium,
                    color = colors.textPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (toastData.actionLabel != null && toastData.onAction != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = toastData.actionLabel,
                        style = GameTheme.typography.labelLarge,
                        color = accentColor,
                        modifier = Modifier
                            .clickable {
                                toastData.onAction.invoke()
                                state.dismiss()
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}
