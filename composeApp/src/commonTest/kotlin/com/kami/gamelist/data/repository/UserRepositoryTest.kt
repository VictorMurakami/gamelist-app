package com.kami.gamelist.data.repository

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import app.cash.turbine.test
import com.kami.gamelist.data.local.GameLocalDataSource
import com.kami.gamelist.data.local.UserLocalDataSource
import com.kami.gamelist.data.model.Game
import com.kami.gamelist.data.model.ListType
import com.kami.gamelist.db.GameListDatabase
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UserRepositoryTest {

    private lateinit var database: GameListDatabase
    private lateinit var userLocalDataSource: UserLocalDataSource
    private lateinit var gameLocalDataSource: GameLocalDataSource
    private lateinit var repository: UserRepository

    private fun sampleGame(id: Int = 1) = Game(
        id = id,
        title = "Game $id",
        thumbnail = "https://example.com/thumb$id.jpg",
        shortDescription = "Description $id",
        gameUrl = "https://example.com/game$id",
        genre = "MMORPG",
        platform = "PC (Windows)",
        publisher = "Publisher",
        developer = "Developer",
        releaseDate = "2023-01-01",
        freetogameProfileUrl = "https://example.com/profile$id"
    )

    @BeforeTest
    fun setup() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        GameListDatabase.Schema.create(driver)
        database = GameListDatabase(driver)
        userLocalDataSource = UserLocalDataSource(database)
        gameLocalDataSource = GameLocalDataSource(database)
        repository = UserRepository(userLocalDataSource)
    }

    @Test
    fun toggleFavoriteAddsAndRemovesGame() = runTest {
        gameLocalDataSource.upsertGames(listOf(sampleGame(1)))

        repository.isFavorite(1).test {
            assertFalse(awaitItem())

            repository.toggleFavorite(1)
            assertTrue(awaitItem())

            repository.toggleFavorite(1)
            assertFalse(awaitItem())

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun observeFavoritesReturnsCorrectGames() = runTest {
        gameLocalDataSource.upsertGames(listOf(sampleGame(1), sampleGame(2)))

        repository.toggleFavorite(1)

        repository.observeFavorites().test {
            val favorites = awaitItem()
            assertEquals(1, favorites.size)
            assertEquals(1, favorites.first().id)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun createAndDeleteList() = runTest {
        val listId = repository.createList("My List", ListType.CUSTOM)

        repository.observeLists().test {
            val lists = awaitItem()
            assertEquals(1, lists.size)
            assertEquals("My List", lists.first().name)

            repository.deleteList(listId)
            val afterDelete = awaitItem()
            assertTrue(afterDelete.isEmpty())

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun addAndRemoveGameFromList() = runTest {
        gameLocalDataSource.upsertGames(listOf(sampleGame(1)))
        val listId = repository.createList("Watchlist", ListType.CUSTOM)

        repository.addToList(listId, 1)

        repository.observeGamesInList(listId).test {
            val games = awaitItem()
            assertEquals(1, games.size)
            assertEquals(1, games.first().id)

            repository.removeFromList(listId, 1)
            val afterRemove = awaitItem()
            assertTrue(afterRemove.isEmpty())

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun addSearchQueryAndObserveRecentSearches() = runTest {
        repository.addSearchQuery("mmorpg")
        repository.addSearchQuery("shooter")

        repository.observeRecentSearches().test {
            val searches = awaitItem()
            assertEquals(2, searches.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun clearSearchHistoryRemovesAllEntries() = runTest {
        repository.addSearchQuery("query1")
        repository.addSearchQuery("query2")

        repository.clearSearchHistory()

        repository.observeRecentSearches().test {
            val searches = awaitItem()
            assertTrue(searches.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
