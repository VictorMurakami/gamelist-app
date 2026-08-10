package com.kami.gamelist.data.local

import com.kami.gamelist.db.GameListDatabase

open class TextPrefDataSource(database: GameListDatabase) {

    private val queries = database.textPrefQueries

    open fun get(key: String): String? = queries.get(key).executeAsOneOrNull()

    fun set(key: String, value: String) = queries.upsert(key, value)

    /**
     * Grava [value] apenas se [key] ainda nao existir (INSERT OR IGNORE).
     *
     * Usado por [com.kami.gamelist.core.config.DeviceIdProvider] para tornar
     * a geracao do device_id atomica: se duas chamadas concorrentes
     * gerarem valores diferentes, so a primeira a chegar no SQLite vence, e
     * a outra deve reler o valor persistido em vez de confiar no que gerou.
     */
    fun setIfAbsent(key: String, value: String) = queries.setIfAbsent(key, value)

    fun delete(key: String) = queries.delete(key)
}
