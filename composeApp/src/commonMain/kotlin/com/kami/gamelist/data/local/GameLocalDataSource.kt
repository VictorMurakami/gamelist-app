package com.kami.gamelist.data.local

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.kami.gamelist.data.model.Game
import com.kami.gamelist.data.model.GameDetail
import com.kami.gamelist.db.GameListDatabase
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class GameLocalDataSource(private val database: GameListDatabase) {

    private val gameQueries = database.gameQueries
    private val screenshotQueries = database.screenshotQueries

    fun observeGames(): Flow<List<Game>> =
        gameQueries.selectAll()
            .asFlow()
            .mapToList(Default)
            .map { entities -> entities.map { it.toDomain() } }

    fun observeGamesByGenre(genre: String): Flow<List<Game>> =
        gameQueries.selectByGenre(genre)
            .asFlow()
            .mapToList(Default)
            .map { entities -> entities.map { it.toDomain() } }

    fun observeGamesByPlatform(platform: String): Flow<List<Game>> =
        gameQueries.selectByPlatform(platform)
            .asFlow()
            .mapToList(Default)
            .map { entities -> entities.map { it.toDomain() } }

    fun observeGamesByGenreAndPlatform(genre: String, platform: String): Flow<List<Game>> =
        gameQueries.selectByGenreAndPlatform(genre, platform)
            .asFlow()
            .mapToList(Default)
            .map { entities -> entities.map { it.toDomain() } }

    fun observeGameById(id: Int): Flow<GameDetail?> {
        val gameFlow = gameQueries.selectById(id.toLong())
            .asFlow()
            .mapToOneOrNull(Default)
        val screenshotsFlow = screenshotQueries.selectByGameId(id.toLong())
            .asFlow()
            .mapToList(Default)

        return combine(gameFlow, screenshotsFlow) { entity, screenshots ->
            entity?.toDetailDomain(screenshots)
        }
    }

    fun observeRecentReleases(limit: Int = 20): Flow<List<Game>> =
        gameQueries.selectRecentReleases(limit.toLong())
            .asFlow()
            .mapToList(Default)
            .map { entities -> entities.map { it.toDomain() } }

    fun observeGamesByIds(ids: List<Int>): Flow<List<Game>> =
        gameQueries.selectByIds(ids.map { it.toLong() })
            .asFlow()
            .mapToList(Default)
            .map { entities ->
                val map = entities.associate { it.id.toInt() to it.toDomain() }
                ids.mapNotNull { map[it] }
            }

    fun observeGamesByGenres(genres: List<String>): Flow<List<Game>> =
        gameQueries.selectByGenres(genres)
            .asFlow()
            .mapToList(Default)
            .map { entities -> entities.map { it.toDomain() } }

    fun searchByTitle(query: String): Flow<List<Game>> =
        gameQueries.searchByTitle(query)
            .asFlow()
            .mapToList(Default)
            .map { entities -> entities.map { it.toDomain() } }

    fun observeGenres(): Flow<List<String>> =
        gameQueries.selectAllGenres()
            .asFlow()
            .mapToList(Default)

    fun observePlatforms(): Flow<List<String>> =
        gameQueries.selectAllPlatforms()
            .asFlow()
            .mapToList(Default)

    fun upsertGames(games: List<Game>) {
        database.transaction {
            games.forEach { game ->
                gameQueries.upsertFromList(
                    id = game.id.toLong(),
                    title = game.title,
                    thumbnail = game.thumbnail,
                    short_description = game.shortDescription,
                    game_url = game.gameUrl,
                    genre = game.genre,
                    platform = game.platform,
                    publisher = game.publisher,
                    developer = game.developer,
                    release_date = game.releaseDate,
                    freetogame_profile_url = game.freetogameProfileUrl
                )
            }
        }
    }

    fun upsertGameDetail(detail: GameDetail) {
        database.transaction {
            val game = detail.game
            val reqs = detail.minimumSystemRequirements
            gameQueries.upsertDetail(
                id = game.id.toLong(),
                title = game.title,
                thumbnail = game.thumbnail,
                short_description = game.shortDescription,
                game_url = game.gameUrl,
                genre = game.genre,
                platform = game.platform,
                publisher = game.publisher,
                developer = game.developer,
                release_date = game.releaseDate,
                freetogame_profile_url = game.freetogameProfileUrl,
                description = detail.description,
                status = detail.status,
                min_req_os = reqs?.os,
                min_req_processor = reqs?.processor,
                min_req_memory = reqs?.memory,
                min_req_graphics = reqs?.graphics,
                min_req_storage = reqs?.storage
            )
            screenshotQueries.deleteByGameId(game.id.toLong())
            detail.screenshots.forEach { ss ->
                screenshotQueries.insertScreenshot(
                    id = ss.id.toLong(),
                    game_id = game.id.toLong(),
                    image = ss.image
                )
            }
        }
    }
}
