package com.kami.gamelist.data.repository

import com.kami.gamelist.data.local.CacheManager
import com.kami.gamelist.data.local.GameLocalDataSource
import com.kami.gamelist.data.model.Game
import com.kami.gamelist.data.model.GameDetail
import com.kami.gamelist.data.remote.FreeToGameApi
import com.kami.gamelist.data.remote.toDomain
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class GameRepository(
    private val api: FreeToGameApi,
    private val localDataSource: GameLocalDataSource,
    private val cacheManager: CacheManager
) {

    fun observeGames(genre: String? = null, platform: String? = null): Flow<List<Game>> = channelFlow {
        val dbFlow = when {
            genre != null && platform != null -> localDataSource.observeGamesByGenreAndPlatform(genre, platform)
            genre != null -> localDataSource.observeGamesByGenre(genre)
            platform != null -> localDataSource.observeGamesByPlatform(platform)
            else -> localDataSource.observeGames()
        }

        // Launch sync in background so DB can emit first (cached) value before sync completes
        if (cacheManager.isStale(CacheManager.GAMES_LIST_KEY, CacheManager.GAMES_LIST_TTL)) {
            launch { syncGames() }
        }

        dbFlow.collectLatest { send(it) }
    }

    fun observeGameDetail(id: Int): Flow<GameDetail?> = channelFlow {
        val key = CacheManager.gameDetailKey(id)
        if (cacheManager.isStale(key, CacheManager.GAME_DETAIL_TTL)) {
            launch { syncGameDetail(id) }
        }
        localDataSource.observeGameById(id).collectLatest { send(it) }
    }

    fun searchGames(query: String): Flow<List<Game>> =
        localDataSource.searchByTitle(query)

    fun observeGenres(): Flow<List<String>> =
        localDataSource.observeGenres()

    fun observePlatforms(): Flow<List<String>> =
        localDataSource.observePlatforms()

    suspend fun refreshGames() {
        syncGames()
    }

    private suspend fun syncGames() {
        try {
            val dtos = api.getGames()
            val games = dtos.map { it.toDomain() }
            localDataSource.upsertGames(games)
            cacheManager.markFetched(CacheManager.GAMES_LIST_KEY)
        } catch (_: Exception) {
            // Silently fail — UI will show cached data or empty state
        }
    }

    private suspend fun syncGameDetail(id: Int) {
        try {
            val dto = api.getGameById(id)
            val detail = dto.toDomain()
            localDataSource.upsertGameDetail(detail)
            cacheManager.markFetched(CacheManager.gameDetailKey(id))
        } catch (_: Exception) {
            // Silently fail — UI will show cached data or empty state
        }
    }
}
