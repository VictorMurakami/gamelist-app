package com.kami.gamelist.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import com.kami.gamelist.core.ui.model.AccentOption
import com.kami.gamelist.core.ui.model.GridColumnsOption
import com.kami.gamelist.core.ui.model.ThemeMode

object GameTheme {
    val colors: GameColorScheme
        @Composable
        @ReadOnlyComposable
        get() = LocalGameColors.current

    val typography: Typography
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.typography
}

val LocalGridColumns = staticCompositionLocalOf { GridColumnsOption.ADAPTIVE }

@Composable
fun GameListTheme(
    themeMode: ThemeMode = ThemeMode.DARK,
    accentOption: AccentOption = AccentOption.CYAN,
    gridColumns: GridColumnsOption = GridColumnsOption.ADAPTIVE,
    content: @Composable () -> Unit,
) {
    val isDark = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val gameColors = if (isDark) darkGameColors(accentOption) else lightGameColors(accentOption)
    val materialScheme = if (isDark) materialDarkScheme(gameColors) else materialLightScheme(gameColors)
    val typography = gameListTypography()

    CompositionLocalProvider(
        LocalGameColors provides gameColors,
        LocalGridColumns provides gridColumns,
    ) {
        MaterialTheme(
            colorScheme = materialScheme,
            typography = typography,
            shapes = GameListShapes,
            content = content
        )
    }
}
