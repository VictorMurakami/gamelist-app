package com.kami.gamelist.feature.home

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import app.cash.turbine.test
import com.kami.gamelist.core.ui.UiState
import com.kami.gamelist.data.local.CacheManager
import com.kami.gamelist.data.local.GameLocalDataSource
import com.kami.gamelist.data.local.UserLocalDataSource
import com.kami.gamelist.data.remote.FreeToGameApi
import com.kami.gamelist.data.remote.dto.GameDto
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class HomeScreenModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var database: GameListDatabase
    private lateinit var repository: GameRepository

    private val sampleDtos = listOf(
        GameDto(
            id = 1, title = "Game A", thumbnail = "https://img.com/1.jpg",
            shortDescription = "Desc", gameUrl = "https://game.com/1",
            genre = "MMORPG", platform = "PC (Windows)", publisher = "Pub",
            developer = "Dev", releaseDate = "2023-01-01",
            freetogameProfileUrl = "https://ftg.com/1"
        )
    )

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        GameListDatabase.Schema.create(driver)
        database = GameListDatabase(driver)

        val client = HttpClient(MockEngine) {
            engine {
                addHandler {
                    respond(
                        content = Json.encodeToString(sampleDtos),
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    )
                }
            }
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }

        repository = GameRepository(
            FreeToGameApi(client),
            GameLocalDataSource(database),
            CacheManager(database)
        )
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialStateIsLoadingThenSuccess() = runTest {
        // Pre-populate DB so observeGames() emits 1 game immediately
        repository.refreshGames()

        val isOnline = MutableStateFlow(true)
        val userRepository = UserRepository(UserLocalDataSource(database))
        val screenModel = HomeScreenModel(repository, userRepository, isOnline)

        screenModel.uiState.test {
            assertIs<UiState.Loading>(awaitItem())
            val success = awaitItem()
            assertIs<UiState.Success<HomeData>>(success)
            assertEquals(1, success.data.games.size)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
