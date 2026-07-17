package com.kami.gamelist.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kami.gamelist.core.ui.modifier.pressScale
import com.kami.gamelist.core.ui.theme.GameTheme

@Composable
fun GameChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = GameTheme.colors

    Surface(
        modifier = modifier
            .height(32.dp)
            .pressScale(onClick = onClick),
        shape = RoundedCornerShape(4.dp),
        color = if (selected) colors.accent.copy(alpha = 0.15f) else colors.surfaceElevated,
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) colors.accent else colors.borderSubtle
        ),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(horizontal = 12.dp)
        ) {
            Text(
                text = label,
                style = GameTheme.typography.labelMedium,
                color = if (selected) colors.accent else colors.textSecondary
            )
        }
    }
}

@Composable
fun GameMultiSelectChipRow(
    options: List<String>,
    selectedOptions: Set<String>,
    onOptionToggled: (String) -> Unit,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier,
    allLabel: String = "All"
) {
    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        GameChip(
            label = allLabel,
            selected = selectedOptions.isEmpty(),
            onClick = onClearAll
        )

        options.forEach { option ->
            GameChip(
                label = option,
                selected = option in selectedOptions,
                onClick = { onOptionToggled(option) }
            )
        }
    }
}

@Composable
fun GameChipRow(
    options: List<String>,
    selectedOption: String?,
    onOptionSelected: (String?) -> Unit,
    modifier: Modifier = Modifier,
    allLabel: String = "All"
) {
    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        GameChip(
            label = allLabel,
            selected = selectedOption == null,
            onClick = { onOptionSelected(null) }
        )

        options.forEach { option ->
            GameChip(
                label = option,
                selected = option == selectedOption,
                onClick = { onOptionSelected(if (option == selectedOption) null else option) }
            )
        }
    }
}
