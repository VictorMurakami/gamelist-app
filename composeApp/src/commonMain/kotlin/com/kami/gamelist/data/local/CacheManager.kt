package com.kami.gamelist.data.local

import com.kami.gamelist.db.GameListDatabase
import kotlinx.datetime.Clock

class CacheManager(private val database: GameListDatabase) {

    companion object {
        const val GAMES_LIST_KEY = "games_list"
        const val GAMES_LIST_TTL = 3_600_000L       // 1 hour
        const val GAME_DETAIL_TTL = 21_600_000L      // 6 hours

        fun gameDetailKey(id: Int) = "game_detail_$id"
    }

    private val queries = database.cacheMetaQueries

    fun isStale(key: String, ttlMillis: Long): Boolean {
        val lastFetched = queries.getLastFetched(key).executeAsOneOrNull() ?: return true
        val elapsed = Clock.System.now().toEpochMilliseconds() - lastFetched
        return elapsed > ttlMillis
    }

    fun markFetched(key: String) {
        queries.upsert(key, Clock.System.now().toEpochMilliseconds())
    }
}
