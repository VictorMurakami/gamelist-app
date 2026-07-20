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
import androidx.compose.material.icons.outlined.Language
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
import com.kami.gamelist.core.ui.localization.AppStrings
import com.kami.gamelist.core.ui.localization.LocalStrings
import com.kami.gamelist.core.ui.components.LocalUserPreferences
import com.kami.gamelist.core.ui.components.GameToastType
import com.kami.gamelist.core.ui.components.LocalGameToastState
import com.kami.gamelist.core.ui.components.SectionHeader
import com.kami.gamelist.core.ui.components.UserPreferencesState
import com.kami.gamelist.core.ui.model.AccentOption
import com.kami.gamelist.core.ui.model.GridColumnsOption
import com.kami.gamelist.core.ui.model.Language
import com.kami.gamelist.core.ui.model.PlatformPreference
import com.kami.gamelist.core.ui.model.ThemeMode
import com.kami.gamelist.core.ui.theme.GameTheme
import kotlinx.coroutines.delay

@Composable
fun SettingsScreen(screenModel: SettingsScreenModel) {
    val toastState = LocalGameToastState.current
    val settings = LocalAppSettings.current
    val colors = GameTheme.colors
    val strings = LocalStrings.current
    var showClearCacheDialog by remember { mutableStateOf(false) }
    var showClearHistoryDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        SectionHeader(title = strings.settings)

        SectionLabel(strings.appearance)

        SettingsItem(
            icon = Icons.Outlined.Palette,
            title = strings.theme,
            subtitle = strings.chooseAppearance,
        )
        OptionChipRow(
            options = ThemeMode.entries.map { it.label(strings) },
            selectedIndex = settings.themeMode.ordinal,
            onSelect = { settings.updateThemeMode(ThemeMode.entries[it]) },
        )

        Spacer(modifier = Modifier.height(16.dp))

        SettingsItem(
            icon = Icons.Outlined.Palette,
            title = strings.accentColor,
            subtitle = strings.primaryColorDesc,
        )
        AccentColorRow(
            selected = settings.accentOption,
            onSelect = {
                settings.updateAccentOption(it)
                toastState.show(strings.accentUpdated, GameToastType.SUCCESS)
            },
        )

        Spacer(modifier = Modifier.height(16.dp))

        SettingsItem(
            icon = Icons.Outlined.GridView,
            title = strings.gridLayout,
            subtitle = strings.gameCardColumns,
        )
        OptionChipRow(
            options = GridColumnsOption.entries.map { it.label(strings) },
            selectedIndex = settings.gridColumns.ordinal,
            onSelect = { settings.updateGridColumns(GridColumnsOption.entries[it]) },
        )

        Spacer(modifier = Modifier.height(16.dp))

        SettingsItem(
            icon = Icons.Outlined.Language,
            title = strings.language,
            subtitle = strings.appLanguage,
        )
        OptionChipRow(
            options = Language.entries.map { it.displayName },
            selectedIndex = settings.language.ordinal,
            onSelect = { settings.updateLanguage(Language.entries[it]) },
        )

        Spacer(modifier = Modifier.height(20.dp))
        HorizontalDivider(color = colors.borderSubtle, modifier = Modifier.padding(horizontal = 16.dp))
        Spacer(modifier = Modifier.height(8.dp))

        PreferencesSection()

        Spacer(modifier = Modifier.height(20.dp))
        HorizontalDivider(color = colors.borderSubtle, modifier = Modifier.padding(horizontal = 16.dp))
        Spacer(modifier = Modifier.height(8.dp))

        SectionLabel(strings.data)

        SettingsItem(
            icon = Icons.Outlined.DeleteSweep,
            title = strings.clearGameCache,
            subtitle = strings.clearGameCacheDesc,
            onClick = { showClearCacheDialog = true }
        )

        HorizontalDivider(color = colors.borderSubtle, modifier = Modifier.padding(horizontal = 16.dp))

        SettingsItem(
            icon = Icons.Outlined.History,
            title = strings.clearSearchHistory,
            subtitle = strings.clearSearchHistoryDesc,
            onClick = { showClearHistoryDialog = true }
        )

        HorizontalDivider(color = colors.borderSubtle, modifier = Modifier.padding(horizontal = 16.dp))

        SettingsItem(
            icon = Icons.Outlined.Refresh,
            title = strings.resetOnboarding,
            subtitle = strings.resetOnboardingDesc,
            onClick = {
                screenModel.resetOnboarding()
                toastState.show(strings.onboardingWillShow, GameToastType.INFO)
            }
        )

        Spacer(modifier = Modifier.height(20.dp))
        HorizontalDivider(color = colors.borderSubtle, modifier = Modifier.padding(horizontal = 16.dp))
        Spacer(modifier = Modifier.height(8.dp))

        SectionLabel(strings.about)

        SettingsItem(
            icon = Icons.Outlined.Info,
            title = strings.appTitle,
            subtitle = strings.aboutDescription,
        )

        Spacer(modifier = Modifier.height(32.dp))
    }

    if (showClearCacheDialog) {
        ConfirmationDialog(
            title = strings.clearCacheQuestion,
            message = strings.clearCacheMessage,
            confirmLabel = strings.delete,
            onConfirm = {
                screenModel.clearGameCache()
                toastState.show(strings.gameCacheCleared, GameToastType.SUCCESS)
            },
            onDismiss = { showClearCacheDialog = false }
        )
    }

    if (showClearHistoryDialog) {
        ConfirmationDialog(
            title = strings.clearHistoryQuestion,
            message = strings.clearHistoryMessage,
            confirmLabel = strings.delete,
            onConfirm = {
                screenModel.clearSearchHistory()
                toastState.show(strings.searchHistoryCleared, GameToastType.SUCCESS)
            },
            onDismiss = { showClearHistoryDialog = false }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PreferencesSection() {
    val colors = GameTheme.colors
    val strings = LocalStrings.current
    val preferences = LocalUserPreferences.current

    val platformLabels = mapOf(
        PlatformPreference.ALL to strings.platformAll,
        PlatformPreference.PC to strings.platformPC,
        PlatformPreference.BROWSER to strings.platformBrowser,
    )

    SectionLabel(strings.preferences)

    SettingsItem(
        icon = Icons.Outlined.Tune,
        title = strings.favoriteGenres,
        subtitle = strings.usedForRecommendations,
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
        title = strings.platform,
        subtitle = strings.filterByPlatform,
    )

    OptionChipRow(
        options = PlatformPreference.entries.map { platformLabels.getValue(it) },
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

private fun ThemeMode.label(strings: AppStrings) = when (this) {
    ThemeMode.DARK -> strings.themeDark
    ThemeMode.LIGHT -> strings.themeLight
    ThemeMode.SYSTEM -> strings.themeSystem
}

private fun GridColumnsOption.label(strings: AppStrings) = when (this) {
    GridColumnsOption.ADAPTIVE -> strings.gridAuto
    GridColumnsOption.TWO -> strings.gridTwoCol
    GridColumnsOption.THREE -> strings.gridThreeCol
}
