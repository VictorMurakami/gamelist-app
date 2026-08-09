package com.kami.gamelist.core.config

/**
 * Acesso tipado as flags. Chave desconhecida e sempre falsa: o app nunca
 * liga uma funcionalidade por nao reconhecer o nome dela.
 */
class FeatureFlags(private val state: AppConfigState) {

    fun isEnabled(key: String): Boolean = state.flags[key] == true
}
