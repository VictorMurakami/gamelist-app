package com.kami.gamelist.core.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

fun materialDarkScheme(colors: GameColorScheme): ColorScheme = darkColorScheme(
    primary = colors.accent,
    secondary = colors.neonPurple,
    tertiary = colors.neonGreen,
    background = colors.backgroundDark,
    surface = colors.surfaceBase,
    surfaceVariant = colors.surfaceElevated,
    error = colors.error,
    errorContainer = colors.errorContainer,
    onErrorContainer = colors.onErrorContainer,
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onTertiary = Color.Black,
    onBackground = colors.textPrimary,
    onSurface = colors.textPrimary,
    onSurfaceVariant = colors.textSecondary,
    outline = colors.borderSubtle,
    outlineVariant = colors.borderSubtle,
)

fun materialLightScheme(colors: GameColorScheme): ColorScheme = lightColorScheme(
    primary = colors.accent,
    secondary = colors.neonPurple,
    tertiary = colors.neonGreen,
    background = colors.backgroundDark,
    surface = colors.surfaceBase,
    surfaceVariant = colors.surfaceElevated,
    error = colors.error,
    errorContainer = colors.errorContainer,
    onErrorContainer = colors.onErrorContainer,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = colors.textPrimary,
    onSurface = colors.textPrimary,
    onSurfaceVariant = colors.textSecondary,
    outline = colors.borderSubtle,
    outlineVariant = colors.borderSubtle,
)
