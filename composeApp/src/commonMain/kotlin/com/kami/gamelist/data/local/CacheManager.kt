package com.kami.gamelist.data.local

import com.kami.gamelist.db.GameListDatabase
import kotlinx.datetime.Clock

class CacheManager(private val database: GameListDatabase) {

    companion object {
        const val GAMES_LIST_KEY = "games_list"
        const val GAMES_LIST_TTL = 3_600_000L       // 1 hour
        const val GAME_DETAIL_TTL = 21_600_000L      // 6 hours

        private const val ONBOARDING_SEEN_KEY = "onboarding_seen"
        private const val POPULAR_COUNT_KEY = "popular_count"
        private const val POPULAR_PREFIX = "popular_"

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

    fun clearAll() {
        queries.deleteAll()
    }

    fun isOnboardingSeen(): Boolean {
        return queries.getLastFetched(ONBOARDING_SEEN_KEY).executeAsOneOrNull() != null
    }

    fun markOnboardingSeen() {
        queries.upsert(ONBOARDING_SEEN_KEY, Clock.System.now().toEpochMilliseconds())
    }

    fun resetOnboarding() {
        queries.delete(ONBOARDING_SEEN_KEY)
    }

    fun getPreference(key: String, default: Int = 0): Int {
        return queries.getLastFetched(key).executeAsOneOrNull()?.toInt() ?: default
    }

    fun setPreference(key: String, value: Int) {
        queries.upsert(key, value.toLong())
    }

    fun getPopularGameIds(): List<Int> {
        val count = getPreference(POPULAR_COUNT_KEY, 0)
        return (0 until count).mapNotNull { i ->
            val id = getPreference("$POPULAR_PREFIX$i", -1)
            if (id >= 0) id else null
        }
    }

    fun setPopularGameIds(ids: List<Int>) {
        setPreference(POPULAR_COUNT_KEY, ids.size)
        ids.forEachIndexed { i, id ->
            setPreference("$POPULAR_PREFIX$i", id)
        }
    }
}
