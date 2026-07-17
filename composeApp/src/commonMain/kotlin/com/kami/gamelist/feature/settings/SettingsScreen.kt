package com.kami.gamelist.feature.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kami.gamelist.core.ui.components.ConfirmationDialog
import com.kami.gamelist.core.ui.components.LocalAppSettings
import com.kami.gamelist.core.ui.components.LocalUserPreferences
import com.kami.gamelist.core.ui.components.GameToastType
import com.kami.gamelist.core.ui.components.LocalGameToastState
import com.kami.gamelist.core.ui.components.SectionHeader
import com.kami.gamelist.core.ui.components.UserPreferencesState
import com.kami.gamelist.core.ui.model.AccentOption
import com.kami.gamelist.core.ui.model.GridColumnsOption
import com.kami.gamelist.core.ui.model.PlatformPreference
import com.kami.gamelist.core.ui.model.ThemeMode
import com.kami.gamelist.core.ui.theme.GameTheme
import kotlinx.coroutines.delay

@Composable
fun SettingsScreen(screenModel: SettingsScreenModel) {
    val toastState = LocalGameToastState.current
    val settings = LocalAppSettings.current
    val colors = GameTheme.colors
    var showClearCacheDialog by remember { mutableStateOf(false) }
    var showClearHistoryDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        SectionHeader(title = "Settings")

        SectionLabel("APPEARANCE")

        SettingsItem(
            icon = Icons.Outlined.Palette,
            title = "Theme",
            subtitle = "Choose app appearance",
        )
        OptionChipRow(
            options = ThemeMode.entries.map { it.label() },
            selectedIndex = settings.themeMode.ordinal,
            onSelect = { settings.updateThemeMode(ThemeMode.entries[it]) },
        )

        Spacer(modifier = Modifier.height(16.dp))

        SettingsItem(
            icon = Icons.Outlined.Palette,
            title = "Accent color",
            subtitle = "Primary color across the app",
        )
        AccentColorRow(
            selected = settings.accentOption,
            onSelect = {
                settings.updateAccentOption(it)
                toastState.show("Accent color updated", GameToastType.SUCCESS)
            },
        )

        Spacer(modifier = Modifier.height(16.dp))

        SettingsItem(
            icon = Icons.Outlined.GridView,
            title = "Grid layout",
            subtitle = "Game card columns",
        )
        OptionChipRow(
            options = GridColumnsOption.entries.map { it.label() },
            selectedIndex = settings.gridColumns.ordinal,
            onSelect = { settings.updateGridColumns(GridColumnsOption.entries[it]) },
        )

        Spacer(modifier = Modifier.height(20.dp))
        HorizontalDivider(color = colors.borderSubtle, modifier = Modifier.padding(horizontal = 16.dp))
        Spacer(modifier = Modifier.height(8.dp))

        PreferencesSection()

        Spacer(modifier = Modifier.height(20.dp))
        HorizontalDivider(color = colors.borderSubtle, modifier = Modifier.padding(horizontal = 16.dp))
        Spacer(modifier = Modifier.height(8.dp))

        SectionLabel("DATA")

        SettingsItem(
            icon = Icons.Outlined.DeleteSweep,
            title = "Clear game cache",
            subtitle = "Forces re-download of game data on next visit",
            onClick = { showClearCacheDialog = true }
        )

        HorizontalDivider(color = colors.borderSubtle, modifier = Modifier.padding(horizontal = 16.dp))

        SettingsItem(
            icon = Icons.Outlined.History,
            title = "Clear search history",
            subtitle = "Removes all recent search queries",
            onClick = { showClearHistoryDialog = true }
        )

        HorizontalDivider(color = colors.borderSubtle, modifier = Modifier.padding(horizontal = 16.dp))

        SettingsItem(
            icon = Icons.Outlined.Refresh,
            title = "Reset onboarding",
            subtitle = "Show the welcome screen again on next launch",
            onClick = {
                screenModel.resetOnboarding()
                toastState.show("Onboarding will show on next launch", GameToastType.INFO)
            }
        )

        Spacer(modifier = Modifier.height(20.dp))
        HorizontalDivider(color = colors.borderSubtle, modifier = Modifier.padding(horizontal = 16.dp))
        Spacer(modifier = Modifier.height(8.dp))

        SectionLabel("ABOUT")

        SettingsItem(
            icon = Icons.Outlined.Info,
            title = "GameList",
            subtitle = "v1.0.0 — Free-to-Play game catalog powered by FreeToGame API",
        )

        Spacer(modifier = Modifier.height(32.dp))
    }

    if (showClearCacheDialog) {
        ConfirmationDialog(
            title = "Clear game cache?",
            message = "Game data will be re-downloaded on next visit.",
            confirmLabel = "Clear",
            onConfirm = {
                screenModel.clearGameCache()
                toastState.show("Game cache cleared", GameToastType.SUCCESS)
            },
            onDismiss = { showClearCacheDialog = false }
        )
    }

    if (showClearHistoryDialog) {
        ConfirmationDialog(
            title = "Clear search history?",
            message = "All recent searches will be removed.",
            confirmLabel = "Clear",
            onConfirm = {
                screenModel.clearSearchHistory()
                toastState.show("Search history cleared", GameToastType.SUCCESS)
            },
            onDismiss = { showClearHistoryDialog = false }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PreferencesSection() {
    val colors = GameTheme.colors
    val preferences = LocalUserPreferences.current

    SectionLabel("PREFERENCES")

    SettingsItem(
        icon = Icons.Outlined.Tune,
        title = "Favorite genres",
        subtitle = "Used for recommendations",
    )

    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        UserPreferencesState.AVAILABLE_GENRES.forEach { genre ->
            val isSelected = genre in preferences.selectedGenres

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (isSelected) colors.accent.copy(alpha = 0.15f) else colors.surfaceElevated,
                border = BorderStroke(1.dp, if (isSelected) colors.accent else colors.borderSubtle),
                modifier = Modifier.clickable { preferences.toggleGenre(genre) }
            ) {
                Text(
                    text = genre,
                    style = GameTheme.typography.labelSmall,
                    color = if (isSelected) colors.accent else colors.textSecondary,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    SettingsItem(
        icon = Icons.Outlined.Tune,
        title = "Platform",
        subtitle = "Filter games by platform",
    )

    OptionChipRow(
        options = PlatformPreference.entries.map { it.label },
        selectedIndex = preferences.platformPreference.ordinal,
        onSelect = { preferences.updatePlatform(PlatformPreference.entries[it]) },
    )
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = GameTheme.typography.labelSmall,
        color = GameTheme.colors.textMuted,
        modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 4.dp)
    )
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: (() -> Unit)? = null,
) {
    val colors = GameTheme.colors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = colors.accent,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = GameTheme.typography.titleSmall, color = colors.textPrimary)
            Text(text = subtitle, style = GameTheme.typography.bodySmall, color = colors.textMuted)
        }
    }
}

@Composable
private fun OptionChipRow(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
) {
    val colors = GameTheme.colors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 56.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = if (selected) colors.accent.copy(alpha = 0.15f) else colors.surfaceElevated,
                border = BorderStroke(
                    1.dp,
                    if (selected) colors.accent else colors.borderSubtle
                ),
                modifier = Modifier
                    .weight(1f)
                    .clickable { onSelect(index) }
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    Text(
                        text = label,
                        style = GameTheme.typography.labelSmall,
                        color = if (selected) colors.accent else colors.textSecondary,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Composable
private fun AccentColorRow(
    selected: AccentOption,
    onSelect: (AccentOption) -> Unit,
) {
    val colors = GameTheme.colors
    val accentColors = mapOf(
        AccentOption.CYAN to Color(0xFF00E5FF),
        AccentOption.PURPLE to Color(0xFFBB86FC),
        AccentOption.PINK to Color(0xFFFF4081),
        AccentOption.GREEN to Color(0xFF00FF88),
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 56.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        AccentOption.entries.forEach { option ->
            val isSelected = option == selected

            var pulse by remember { mutableStateOf(false) }
            LaunchedEffect(isSelected) {
                if (isSelected) {
                    pulse = true
                    delay(300)
                    pulse = false
                }
            }
            val scale by animateFloatAsState(
                targetValue = if (pulse) 1.15f else 1f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                label = "accent_pulse"
            )
            val borderWidth by animateDpAsState(
                targetValue = if (isSelected) 3.dp else 1.dp,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                label = "accent_border"
            )
            val borderColor by animateColorAsState(
                targetValue = if (isSelected) colors.textPrimary else colors.borderSubtle,
                label = "accent_border_color"
            )

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
                    .clip(CircleShape)
                    .background(accentColors.getValue(option), CircleShape)
                    .border(borderWidth, borderColor, CircleShape)
                    .clickable { onSelect(option) }
            )
        }
    }
}

private fun ThemeMode.label() = when (this) {
    ThemeMode.DARK -> "Dark"
    ThemeMode.LIGHT -> "Light"
    ThemeMode.SYSTEM -> "System"
}

private fun GridColumnsOption.label() = when (this) {
    GridColumnsOption.ADAPTIVE -> "Auto"
    GridColumnsOption.TWO -> "2 Col"
    GridColumnsOption.THREE -> "3 Col"
}
