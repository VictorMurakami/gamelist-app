package com.kami.gamelist.core.config

/**
 * Identificacao do app enviada ao backend no app-config.
 *
 * `platform` precisa ser exatamente "android" ou "ios" — o backend valida
 * com ChoiceField e responde 400 para qualquer outro valor.
 */
data class AppInfo(
    val platform: String,
    val version: String,
)

expect fun currentAppInfo(): AppInfo
