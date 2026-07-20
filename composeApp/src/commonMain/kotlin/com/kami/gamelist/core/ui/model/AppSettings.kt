package com.kami.gamelist.core.ui.model

enum class ThemeMode { DARK, LIGHT, SYSTEM }

enum class AccentOption { CYAN, PURPLE, PINK, GREEN }

enum class GridColumnsOption { ADAPTIVE, TWO, THREE }

enum class PlatformPreference {
    ALL, PC, BROWSER
}

enum class Language(val displayName: String) {
    EN("English"),
    PT_BR("Português (BR)"),
}
