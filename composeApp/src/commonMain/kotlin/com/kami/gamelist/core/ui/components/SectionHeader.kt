package com.kami.gamelist.core.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kami.gamelist.core.ui.theme.GameTheme

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    horizontalPadding: Dp = 16.dp,
) {
    Text(
        text = title.uppercase(),
        style = GameTheme.typography.headlineMedium,
        color = GameTheme.colors.textPrimary,
        modifier = modifier
            .padding(horizontal = horizontalPadding)
            .padding(top = 16.dp, bottom = 8.dp)
    )
}
