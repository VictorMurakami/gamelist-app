package com.kami.gamelist.data.local

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import app.cash.turbine.test
import com.kami.gamelist.data.model.Game
import com.kami.gamelist.data.model.ListType
import com.kami.gamelist.db.GameListDatabase
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UserLocalDataSourceTest {

    private lateinit var database: GameListDatabase
    private lateinit var gameDataSource: GameLocalDataSource
    private lateinit var userDataSource: UserLocalDataSource

    @BeforeTest
    fun setup() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        GameListDatabase.Schema.create(driver)
        database = GameListDatabase(driver)
        gameDataSource = GameLocalDataSource(database)
        userDataSource = UserLocalDataSource(database)
    }

    private fun sampleGame(id: Int = 1) = Game(
        id = id,
        title = "Game $id",
        thumbnail = "https://example.com/thumb.jpg",
        shortDescription = "Desc",
        gameUrl = "https://example.com",
        genre = "RPG",
        platform = "PC (Windows)",
        publisher = "Pub",
        developer = "Dev",
        releaseDate = "2023-01-01",
        freetogameProfileUrl = "https://example.com/profile"
    )

    @Test
    fun toggleFavoriteAddsAndRemoves() = runTest {
        gameDataSource.upsertGames(listOf(sampleGame(1)))

        userDataSource.toggleFavorite(1)
        userDataSource.isFavorite(1).test {
            assertTrue(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }

        userDataSource.toggleFavorite(1)
        userDataSource.isFavorite(1).test {
            assertFalse(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun observeFavoritesReturnsGames() = runTest {
        gameDataSource.upsertGames(listOf(sampleGame(1), sampleGame(2)))
        userDataSource.toggleFavorite(1)
        userDataSource.toggleFavorite(2)

        userDataSource.observeFavorites().test {
            val result = awaitItem()
            assertEquals(2, result.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun createAndObserveLists() = runTest {
        userDataSource.createList("Jogando", ListType.PLAYING)
        userDataSource.createList("Quero Jogar", ListType.WANT_TO_PLAY)

        userDataSource.observeLists().test {
            val result = awaitItem()
            assertEquals(2, result.size)
            assertEquals("Jogando", result[0].name)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun addAndRemoveFromList() = runTest {
        gameDataSource.upsertGames(listOf(sampleGame(1)))
        val listId = userDataSource.createList("Jogando", ListType.PLAYING)

        userDataSource.addToList(listId, 1)
        userDataSource.observeGamesInList(listId).test {
            val result = awaitItem()
            assertEquals(1, result.size)
            assertEquals("Game 1", result[0].title)
            cancelAndIgnoreRemainingEvents()
        }

        userDataSource.removeFromList(listId, 1)
        userDataSource.observeGamesInList(listId).test {
            val result = awaitItem()
            assertEquals(0, result.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun searchHistoryStoresAndRetrievesRecent() = runTest {
        userDataSource.addSearchQuery("genshin", searchedAt = 1000L)
        userDataSource.addSearchQuery("lost ark", searchedAt = 2000L)

        userDataSource.observeRecentSearches().test {
            val result = awaitItem()
            assertEquals(2, result.size)
            assertEquals("lost ark", result[0].query)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
