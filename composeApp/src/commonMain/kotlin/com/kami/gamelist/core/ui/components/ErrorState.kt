package com.kami.gamelist.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kami.gamelist.core.ui.localization.LocalStrings
import com.kami.gamelist.core.ui.modifier.pressScale
import com.kami.gamelist.core.ui.theme.GameTheme

@Composable
fun ErrorState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = GameTheme.colors
    val strings = LocalStrings.current

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.Warning,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = colors.error
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = message,
            style = GameTheme.typography.bodyLarge,
            color = colors.textSecondary
        )

        Spacer(modifier = Modifier.height(16.dp))

        Surface(
            modifier = Modifier
                .fillMaxWidth(0.5f)
                .height(44.dp)
                .pressScale(onClick = onRetry),
            shape = RoundedCornerShape(6.dp),
            color = colors.accent.copy(alpha = 0.1f),
            border = BorderStroke(1.dp, colors.accent),
            tonalElevation = 0.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = strings.retry,
                    style = GameTheme.typography.labelLarge,
                    color = colors.accent
                )
            }
        }
    }
}
