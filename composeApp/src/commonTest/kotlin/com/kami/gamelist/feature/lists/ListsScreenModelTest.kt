package com.kami.gamelist.feature.lists

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import app.cash.turbine.test
import com.kami.gamelist.data.local.UserLocalDataSource
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
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ListsScreenModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var userRepository: UserRepository

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        GameListDatabase.Schema.create(driver)
        val database = GameListDatabase(driver)
        userRepository = UserRepository(UserLocalDataSource(database))
    }

    @AfterTest
    fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun createAndObserveLists() = runTest {
        val screenModel = ListsScreenModel(userRepository)
        screenModel.createList("Jogando")
        screenModel.createList("Quero Jogar")

        screenModel.lists.test {
            var result = awaitItem()
            if (result.isEmpty()) {
                testDispatcher.scheduler.advanceUntilIdle()
                result = awaitItem()
            }
            assertEquals(2, result.size)
            val names = result.map { it.name }.toSet()
            assertTrue(names.contains("Jogando"))
            assertTrue(names.contains("Quero Jogar"))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun deleteListRemovesIt() = runTest {
        val screenModel = ListsScreenModel(userRepository)
        screenModel.createList("Temp")

        screenModel.lists.test {
            var before = awaitItem()
            if (before.isEmpty()) {
                testDispatcher.scheduler.advanceUntilIdle()
                before = awaitItem()
            }
            assertEquals(1, before.size)

            screenModel.deleteList(before[0].id)
            testDispatcher.scheduler.advanceUntilIdle()
            val after = awaitItem()
            assertEquals(0, after.size)

            cancelAndIgnoreRemainingEvents()
        }
    }
}
