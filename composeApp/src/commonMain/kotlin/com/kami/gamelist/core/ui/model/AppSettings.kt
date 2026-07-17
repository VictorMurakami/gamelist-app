package com.kami.gamelist.core.ui.model

enum class ThemeMode { DARK, LIGHT, SYSTEM }

enum class AccentOption { CYAN, PURPLE, PINK, GREEN }

enum class GridColumnsOption { ADAPTIVE, TWO, THREE }

enum class PlatformPreference(val label: String) {
    ALL("All"),
    PC("PC"),
    BROWSER("Browser")
}
