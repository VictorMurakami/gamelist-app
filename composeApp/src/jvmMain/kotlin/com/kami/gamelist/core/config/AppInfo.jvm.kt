package com.kami.gamelist.core.config

// O target jvm existe para rodar os testes; nao e um app publicavel.
actual fun currentAppInfo(): AppInfo = AppInfo(platform = "android", version = "1.0.0")
