package com.kami.gamelist.core.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.kami.gamelist.core.ui.model.AccentOption

@Immutable
data class GameColorScheme(
    val backgroundDark: Color,
    val surfaceBase: Color,
    val surfaceElevated: Color,
    val surfaceOverlay: Color,

    val accent: Color,

    val neonCyan: Color,
    val neonPurple: Color,
    val neonPink: Color,
    val neonGreen: Color,

    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,

    val error: Color,
    val errorContainer: Color,
    val onErrorContainer: Color,

    val borderSubtle: Color,
    val borderAccent: Color,
)

private val BaseDarkColors = GameColorScheme(
    backgroundDark = Color(0xFF0A0A14),
    surfaceBase = Color(0xFF12121E),
    surfaceElevated = Color(0xFF1A1A2E),
    surfaceOverlay = Color(0xFF222238),

    accent = Color(0xFF00E5FF),

    neonCyan = Color(0xFF00E5FF),
    neonPurple = Color(0xFFBB86FC),
    neonPink = Color(0xFFFF4081),
    neonGreen = Color(0xFF00FF88),

    textPrimary = Color(0xFFE8E8F0),
    textSecondary = Color(0xFFB0B0C0),
    textMuted = Color(0xFF6E6E80),

    error = Color(0xFFFF3366),
    errorContainer = Color(0xFF3D1020),
    onErrorContainer = Color(0xFFFFB4C4),

    borderSubtle = Color(0xFF2A2A3E),
    borderAccent = Color(0x4D00E5FF),
)

private val BaseLightColors = GameColorScheme(
    backgroundDark = Color(0xFFF0F0F6),
    surfaceBase = Color(0xFFFFFFFF),
    surfaceElevated = Color(0xFFE8E8EE),
    surfaceOverlay = Color(0xFFD8D8E2),

    accent = Color(0xFF0097A7),

    neonCyan = Color(0xFF0097A7),
    neonPurple = Color(0xFF7B1FA2),
    neonPink = Color(0xFFC2185B),
    neonGreen = Color(0xFF00897B),

    textPrimary = Color(0xFF1A1A2E),
    textSecondary = Color(0xFF4A4A5E),
    textMuted = Color(0xFF7E7E90),

    error = Color(0xFFD32F2F),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),

    borderSubtle = Color(0xFFD0D0DA),
    borderAccent = Color(0x4D0097A7),
)

private val DarkAccents = mapOf(
    AccentOption.CYAN to Color(0xFF00E5FF),
    AccentOption.PURPLE to Color(0xFFBB86FC),
    AccentOption.PINK to Color(0xFFFF4081),
    AccentOption.GREEN to Color(0xFF00FF88),
)

private val LightAccents = mapOf(
    AccentOption.CYAN to Color(0xFF0097A7),
    AccentOption.PURPLE to Color(0xFF7B1FA2),
    AccentOption.PINK to Color(0xFFC2185B),
    AccentOption.GREEN to Color(0xFF00897B),
)

fun darkGameColors(accent: AccentOption = AccentOption.CYAN): GameColorScheme {
    val accentColor = DarkAccents.getValue(accent)
    return BaseDarkColors.copy(
        accent = accentColor,
        borderAccent = accentColor.copy(alpha = 0.3f),
    )
}

fun lightGameColors(accent: AccentOption = AccentOption.CYAN): GameColorScheme {
    val accentColor = LightAccents.getValue(accent)
    return BaseLightColors.copy(
        accent = accentColor,
        borderAccent = accentColor.copy(alpha = 0.3f),
    )
}

val DarkGameColors = darkGameColors()

val LocalGameColors = staticCompositionLocalOf { DarkGameColors }
