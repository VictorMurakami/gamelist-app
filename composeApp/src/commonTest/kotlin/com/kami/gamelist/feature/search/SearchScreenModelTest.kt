package com.kami.gamelist.feature.search

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import app.cash.turbine.test
import com.kami.gamelist.data.local.CacheManager
import com.kami.gamelist.data.local.GameLocalDataSource
import com.kami.gamelist.data.local.UserLocalDataSource
import com.kami.gamelist.data.model.Game
import com.kami.gamelist.data.remote.FreeToGameApi
import com.kami.gamelist.data.repository.GameRepository
import com.kami.gamelist.data.repository.UserRepository
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.Json
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class SearchScreenModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var database: GameListDatabase
    private lateinit var gameRepository: GameRepository
    private lateinit var userRepository: UserRepository

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        GameListDatabase.Schema.create(driver)
        database = GameListDatabase(driver)

        val client = HttpClient(MockEngine) {
            engine { addHandler { respond("[]", HttpStatusCode.OK,
                headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())) } }
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }

        val localDataSource = GameLocalDataSource(database)
        gameRepository = GameRepository(FreeToGameApi(client), localDataSource, CacheManager(database))
        userRepository = UserRepository(UserLocalDataSource(database))

        localDataSource.upsertGames(listOf(
            Game(1, "Genshin Impact", "", "desc", "", "MMORPG", "PC", "Pub", "Dev", "2023-01-01", ""),
            Game(2, "Lost Ark", "", "desc", "", "MMORPG", "PC", "Pub", "Dev", "2023-01-01", "")
        ))
    }

    @AfterTest
    fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun searchFiltersGames() = runTest {
        val screenModel = SearchScreenModel(gameRepository, userRepository)

        screenModel.searchResults.test {
            awaitItem() // initial empty list

            screenModel.onQueryChange("Genshin")
            screenModel.onSearch("Genshin")
            testDispatcher.scheduler.advanceTimeBy(350)
            testDispatcher.scheduler.advanceUntilIdle()

            val result = awaitItem()
            assertEquals(1, result.size)
            assertEquals("Genshin Impact", result[0].title)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun searchSavesToHistory() = runTest {
        val screenModel = SearchScreenModel(gameRepository, userRepository)
        screenModel.onSearch("Genshin")

        screenModel.recentSearches.test {
            // stateIn with WhileSubscribed emits initial empty, then the DB update
            var result = awaitItem()
            if (result.isEmpty()) {
                testDispatcher.scheduler.advanceUntilIdle()
                result = awaitItem()
            }
            assertEquals(1, result.size)
            assertEquals("Genshin", result[0].query)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
