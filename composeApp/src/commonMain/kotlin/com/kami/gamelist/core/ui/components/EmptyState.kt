package com.kami.gamelist.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kami.gamelist.core.ui.theme.GameTheme

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val colors = GameTheme.colors

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = colors.textMuted
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = title,
            style = GameTheme.typography.titleMedium,
            color = colors.textSecondary
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = subtitle,
            style = GameTheme.typography.bodyMedium,
            color = colors.textMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )

        if (actionLabel != null && onAction != null) {
            Spacer(modifier = Modifier.height(20.dp))
            OutlinedButton(
                onClick = onAction,
                shape = RoundedCornerShape(6.dp),
                border = BorderStroke(1.dp, colors.accent),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = colors.accent
                )
            ) {
                Text(
                    text = actionLabel,
                    style = GameTheme.typography.labelLarge
                )
            }
        }
    }
}
