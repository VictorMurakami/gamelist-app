package com.kami.gamelist.feature.detail

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import app.cash.turbine.test
import com.kami.gamelist.core.ui.UiState
import com.kami.gamelist.data.local.CacheManager
import com.kami.gamelist.data.local.GameLocalDataSource
import com.kami.gamelist.data.local.UserLocalDataSource
import com.kami.gamelist.data.model.Game
import com.kami.gamelist.data.model.GameDetail
import com.kami.gamelist.data.remote.FreeToGameApi
import com.kami.gamelist.data.remote.dto.GameDetailDto
import com.kami.gamelist.data.remote.dto.MinimumSystemRequirementsDto
import com.kami.gamelist.data.remote.dto.ScreenshotDto
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
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class GameDetailScreenModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var database: GameListDatabase
    private lateinit var gameRepository: GameRepository
    private lateinit var userRepository: UserRepository

    private val detailDto = GameDetailDto(
        id = 1, title = "Game 1", thumbnail = "https://img.com/1.jpg",
        shortDescription = "Desc", gameUrl = "https://game.com/1",
        genre = "MMORPG", platform = "PC", publisher = "Pub",
        developer = "Dev", releaseDate = "2023-01-01",
        freetogameProfileUrl = "https://ftg.com/1",
        description = "Full desc", status = "Live",
        screenshots = listOf(ScreenshotDto(1, "https://img.com/ss1.jpg")),
        minimumSystemRequirements = MinimumSystemRequirementsDto("Win10", "i5", "8GB", "GTX1060", "30GB")
    )

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        GameListDatabase.Schema.create(driver)
        database = GameListDatabase(driver)

        val client = HttpClient(MockEngine) {
            engine { addHandler { respond(Json.encodeToString(detailDto), HttpStatusCode.OK,
                headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())) } }
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }

        val localDataSource = GameLocalDataSource(database)
        gameRepository = GameRepository(FreeToGameApi(client), localDataSource, CacheManager(database))
        userRepository = UserRepository(UserLocalDataSource(database))
    }

    @AfterTest
    fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun loadsGameDetailSuccessfully() = runTest {
        val screenModel = GameDetailScreenModel(1, gameRepository, userRepository)

        screenModel.uiState.test {
            assertIs<UiState.Loading>(awaitItem())
            val success = awaitItem()
            assertIs<UiState.Success<GameDetail>>(success)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun toggleFavoriteUpdatesFavoriteState() = runTest {
        val localDataSource = GameLocalDataSource(database)
        localDataSource.upsertGames(listOf(
            Game(1, "Game 1", "", "desc", "", "MMORPG", "PC", "Pub", "Dev", "2023-01-01", "")
        ))

        val screenModel = GameDetailScreenModel(1, gameRepository, userRepository)

        screenModel.isFavorite.test {
            val initial = awaitItem()
            assertTrue(!initial)

            screenModel.toggleFavorite()
            testDispatcher.scheduler.advanceUntilIdle()

            assertTrue(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
