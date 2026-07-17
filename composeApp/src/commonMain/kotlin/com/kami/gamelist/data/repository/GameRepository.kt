package com.kami.gamelist.data.repository

import com.kami.gamelist.data.local.CacheManager
import com.kami.gamelist.data.local.GameLocalDataSource
import com.kami.gamelist.data.model.Game
import com.kami.gamelist.data.model.GameDetail
import com.kami.gamelist.data.remote.FreeToGameApi
import com.kami.gamelist.data.remote.SortOption
import com.kami.gamelist.data.remote.toDomain
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class GameRepository(
    private val api: FreeToGameApi,
    private val localDataSource: GameLocalDataSource,
    private val cacheManager: CacheManager
) {

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    private val _popularIds = MutableStateFlow(cacheManager.getPopularGameIds())

    fun observeGames(genre: String? = null, platform: String? = null): Flow<List<Game>> = channelFlow {
        val dbFlow = when {
            genre != null && platform != null -> localDataSource.observeGamesByGenreAndPlatform(genre, platform)
            genre != null -> localDataSource.observeGamesByGenre(genre)
            platform != null -> localDataSource.observeGamesByPlatform(platform)
            else -> localDataSource.observeGames()
        }

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

    suspend fun refreshGameDetail(id: Int) {
        syncGameDetail(id)
    }

    fun observeGamesFiltered(
        genres: Set<String> = emptySet(),
        platform: String? = null,
    ): Flow<List<Game>> = channelFlow {
        val dbFlow = when {
            genres.isNotEmpty() -> localDataSource.observeGamesByGenres(genres.toList())
                .map { games ->
                    if (platform != null) games.filter { it.platform.contains(platform, ignoreCase = true) }
                    else games
                }
            platform != null -> localDataSource.observeGamesByPlatform(platform)
            else -> localDataSource.observeGames()
        }

        if (cacheManager.isStale(CacheManager.GAMES_LIST_KEY, CacheManager.GAMES_LIST_TTL)) {
            launch { syncGames() }
        }

        dbFlow.collectLatest { send(it) }
    }

    fun observeRecentReleases(limit: Int = 20): Flow<List<Game>> =
        localDataSource.observeRecentReleases(limit)

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observePopularGames(): Flow<List<Game>> =
        _popularIds.flatMapLatest { ids ->
            if (ids.isEmpty()) flowOf(emptyList())
            else localDataSource.observeGamesByIds(ids)
        }

    fun observeRecommendedGames(genres: List<String>, limit: Int = 20): Flow<List<Game>> {
        if (genres.isEmpty()) return flowOf(emptyList())
        return localDataSource.observeGamesByGenres(genres)
            .map { it.take(limit) }
    }

    suspend fun syncPopularGames() {
        try {
            val dtos = api.getGames(sortBy = SortOption.POPULARITY)
            val top = dtos.take(20)
            localDataSource.upsertGames(top.map { it.toDomain() })
            val ids = top.map { it.id }
            cacheManager.setPopularGameIds(ids)
            _popularIds.value = ids
        } catch (_: Exception) { }
    }

    fun clearCache() {
        cacheManager.clearAll()
    }

    private suspend fun syncGames() {
        _syncState.value = SyncState.Syncing
        try {
            val dtos = api.getGames()
            val games = dtos.map { it.toDomain() }
            localDataSource.upsertGames(games)
            cacheManager.markFetched(CacheManager.GAMES_LIST_KEY)
            _syncState.value = SyncState.Synced
        } catch (e: Exception) {
            _syncState.value = SyncState.SyncFailed(e.message ?: "Sync failed")
        }
    }

    private suspend fun syncGameDetail(id: Int) {
        _syncState.value = SyncState.Syncing
        try {
            val dto = api.getGameById(id)
            val detail = dto.toDomain()
            localDataSource.upsertGameDetail(detail)
            cacheManager.markFetched(CacheManager.gameDetailKey(id))
            _syncState.value = SyncState.Synced
        } catch (e: Exception) {
            _syncState.value = SyncState.SyncFailed(e.message ?: "Sync failed")
        }
    }
}
