package com.kami.gamelist.core.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

val Purple80 = Color(0xFFBB86FC)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Cyan80 = Color(0xFF80DEEA)

val Purple40 = Color(0xFF6200EE)
val PurpleGrey40 = Color(0xFF625B71)
val Cyan40 = Color(0xFF00838F)

val DarkBackground = Color(0xFF0F0F1A)
val DarkSurface = Color(0xFF1A1A2E)
val DarkSurfaceVariant = Color(0xFF252540)

val AccentCyan = Color(0xFF00E5FF)
val AccentPurple = Color(0xFFBB86FC)

val DarkColorScheme = darkColorScheme(
    primary = AccentCyan,
    secondary = AccentPurple,
    tertiary = Cyan80,
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceVariant,
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White,
    onSurfaceVariant = Color(0xFFB0B0C0)
)

val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = Cyan40,
    tertiary = PurpleGrey40,
    background = Color(0xFFF8F8FC),
    surface = Color.White,
    surfaceVariant = Color(0xFFEEEEF4),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFF1A1A2E),
    onSurface = Color(0xFF1A1A2E),
    onSurfaceVariant = Color(0xFF555570)
)
