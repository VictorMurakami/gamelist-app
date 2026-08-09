package com.kami.gamelist.core.config

import com.kami.gamelist.BuildConfig

actual fun currentAppInfo(): AppInfo = AppInfo(
    platform = "android",
    version = BuildConfig.VERSION_NAME,
)
