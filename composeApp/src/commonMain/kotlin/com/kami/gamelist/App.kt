package com.kami.gamelist

import androidx.compose.runtime.Composable
import com.kami.gamelist.core.ui.theme.GameListTheme
import com.kami.gamelist.feature.navigation.AppNavigator

@Composable
fun App() {
    GameListTheme(darkTheme = true) {
        AppNavigator()
    }
}
