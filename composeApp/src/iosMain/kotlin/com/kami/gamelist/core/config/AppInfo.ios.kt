package com.kami.gamelist.core.config

import platform.Foundation.NSBundle

actual fun currentAppInfo(): AppInfo {
    val version = NSBundle.mainBundle.objectForInfoDictionaryKey(
        "CFBundleShortVersionString"
    ) as? String

    return AppInfo(platform = "ios", version = version ?: "1.0.0")
}
