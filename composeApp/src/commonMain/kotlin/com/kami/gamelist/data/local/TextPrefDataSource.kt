package com.kami.gamelist.data.local

import com.kami.gamelist.db.GameListDatabase

class TextPrefDataSource(database: GameListDatabase) {

    private val queries = database.textPrefQueries

    fun get(key: String): String? = queries.get(key).executeAsOneOrNull()

    fun set(key: String, value: String) = queries.upsert(key, value)

    fun delete(key: String) = queries.delete(key)
}
