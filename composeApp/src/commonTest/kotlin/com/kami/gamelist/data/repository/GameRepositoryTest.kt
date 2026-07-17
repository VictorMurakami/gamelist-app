package com.kami.gamelist.data.repository

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import app.cash.turbine.test
import com.kami.gamelist.data.local.CacheManager
import com.kami.gamelist.data.local.GameLocalDataSource
import com.kami.gamelist.data.remote.FreeToGameApi
import com.kami.gamelist.data.remote.dto.GameDto
import com.kami.gamelist.db.GameListDatabase
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GameRepositoryTest {

    private lateinit var database: GameListDatabase
    private lateinit var localDataSource: GameLocalDataSource
    private lateinit var cacheManager: CacheManager
    private lateinit var repository: GameRepository

    private val sampleDtos = listOf(
        GameDto(
            id = 1, title = "Game 1", thumbnail = "https://img.com/1.jpg",
            shortDescription = "Desc 1", gameUrl = "https://game.com/1",
            genre = "MMORPG", platform = "PC (Windows)", publisher = "Pub",
            developer = "Dev", releaseDate = "2023-01-01",
            freetogameProfileUrl = "https://ftg.com/1"
        ),
        GameDto(
            id = 2, title = "Game 2", thumbnail = "https://img.com/2.jpg",
            shortDescription = "Desc 2", gameUrl = "https://game.com/2",
            genre = "Shooter", platform = "Web Browser", publisher = "Pub",
            developer = "Dev", releaseDate = "2023-02-01",
            freetogameProfileUrl = "https://ftg.com/2"
        )
    )

    private fun createApiWithResponse(json: String): FreeToGameApi {
        val client = HttpClient(MockEngine) {
            engine {
                addHandler {
                    respond(
                        content = json,
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    )
                }
            }
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }
        return FreeToGameApi(client)
    }

    @BeforeTest
    fun setup() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        GameListDatabase.Schema.create(driver)
        database = GameListDatabase(driver)
        localDataSource = GameLocalDataSource(database)
        cacheManager = CacheManager(database)
    }

    @Test
    fun observeGamesReturnsCachedDataThenSyncsFromApi() = runTest {
        val api = createApiWithResponse(Json.encodeToString(sampleDtos))
        repository = GameRepository(api, localDataSource, cacheManager)

        repository.observeGames().test {
            val first = awaitItem()
            // Initially empty (no cache)
            assertTrue(first.isEmpty())

            // After sync, should have 2 games
            val second = awaitItem()
            assertEquals(2, second.size)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun refreshGamesUpdatesLocalData() = runTest {
        val api = createApiWithResponse(Json.encodeToString(sampleDtos))
        repository = GameRepository(api, localDataSource, cacheManager)

        repository.refreshGames()

        localDataSource.observeGames().test {
            val result = awaitItem()
            assertEquals(2, result.size)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
