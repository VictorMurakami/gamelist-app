package com.kami.gamelist.core.ui.components

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf

@Stable
class ScrollToTopState {
    var trigger by mutableStateOf(0)
        private set

    fun requestScrollToTop() {
        trigger++
    }
}

val LocalScrollToTop = staticCompositionLocalOf { ScrollToTopState() }
