package com.kami.gamelist.core.ui.components

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import com.kami.gamelist.core.ui.model.AccentOption
import com.kami.gamelist.core.ui.model.GridColumnsOption
import com.kami.gamelist.core.ui.model.ThemeMode
import com.kami.gamelist.data.local.CacheManager

@Stable
class AppSettingsState(private val cacheManager: CacheManager) {

    companion object {
        private const val KEY_THEME = "pref_theme_mode"
        private const val KEY_ACCENT = "pref_accent_color"
        private const val KEY_GRID = "pref_grid_columns"
    }

    var themeMode by mutableStateOf(
        ThemeMode.entries.getOrElse(cacheManager.getPreference(KEY_THEME)) { ThemeMode.DARK }
    )
        private set

    var accentOption by mutableStateOf(
        AccentOption.entries.getOrElse(cacheManager.getPreference(KEY_ACCENT)) { AccentOption.CYAN }
    )
        private set

    var gridColumns by mutableStateOf(
        GridColumnsOption.entries.getOrElse(cacheManager.getPreference(KEY_GRID)) { GridColumnsOption.ADAPTIVE }
    )
        private set

    fun updateThemeMode(mode: ThemeMode) {
        themeMode = mode
        cacheManager.setPreference(KEY_THEME, mode.ordinal)
    }

    fun updateAccentOption(accent: AccentOption) {
        accentOption = accent
        cacheManager.setPreference(KEY_ACCENT, accent.ordinal)
    }

    fun updateGridColumns(columns: GridColumnsOption) {
        gridColumns = columns
        cacheManager.setPreference(KEY_GRID, columns.ordinal)
    }
}

val LocalAppSettings = staticCompositionLocalOf<AppSettingsState> {
    error("No AppSettingsState provided")
}
