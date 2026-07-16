package com.kami.gamelist.feature.favorites

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import app.cash.turbine.test
import com.kami.gamelist.data.local.GameLocalDataSource
import com.kami.gamelist.data.local.UserLocalDataSource
import com.kami.gamelist.data.model.Game
import com.kami.gamelist.data.repository.UserRepository
import com.kami.gamelist.db.GameListDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class FavoritesScreenModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var database: GameListDatabase
    private lateinit var userRepository: UserRepository

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        GameListDatabase.Schema.create(driver)
        database = GameListDatabase(driver)

        val gameDataSource = GameLocalDataSource(database)
        gameDataSource.upsertGames(listOf(
            Game(1, "Game 1", "", "desc", "", "RPG", "PC", "Pub", "Dev", "2023-01-01", ""),
            Game(2, "Game 2", "", "desc", "", "FPS", "PC", "Pub", "Dev", "2023-01-01", "")
        ))

        val userDataSource = UserLocalDataSource(database)
        userDataSource.toggleFavorite(1)
        userDataSource.toggleFavorite(2)
        userRepository = UserRepository(userDataSource)
    }

    @AfterTest
    fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun showsFavoritedGames() = runTest {
        val screenModel = FavoritesScreenModel(userRepository)
        screenModel.favorites.test {
            var result = awaitItem()
            if (result.isEmpty()) {
                testDispatcher.scheduler.advanceUntilIdle()
                result = awaitItem()
            }
            assertEquals(2, result.size)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
