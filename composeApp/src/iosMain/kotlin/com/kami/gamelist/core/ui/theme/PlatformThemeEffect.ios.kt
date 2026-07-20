package com.kami.gamelist.core.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import platform.Foundation.NSNumber
import platform.Foundation.setValue
import platform.UIKit.UIApplication
import platform.UIKit.UIWindow
import platform.UIKit.UIWindowScene

@Composable
actual fun PlatformThemeEffect(isDark: Boolean) {
    DisposableEffect(isDark) {
        val styleValue = NSNumber(long = if (isDark) 2L else 1L)
        for (scene in UIApplication.sharedApplication.connectedScenes) {
            val windowScene = scene as? UIWindowScene ?: continue
            for (window in windowScene.windows) {
                (window as UIWindow).setValue(styleValue, forKey = "overrideUserInterfaceStyle")
            }
        }
        onDispose { }
    }
}
