package com.kami.gamelist.data.local

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import app.cash.turbine.test
import com.kami.gamelist.data.model.Game
import com.kami.gamelist.data.model.GameDetail
import com.kami.gamelist.data.model.Screenshot
import com.kami.gamelist.data.model.SystemRequirements
import com.kami.gamelist.db.GameListDatabase
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class GameLocalDataSourceTest {

    private lateinit var database: GameListDatabase
    private lateinit var dataSource: GameLocalDataSource

    @BeforeTest
    fun setup() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        GameListDatabase.Schema.create(driver)
        database = GameListDatabase(driver)
        dataSource = GameLocalDataSource(database)
    }

    private fun sampleGame(id: Int = 1, genre: String = "MMORPG") = Game(
        id = id,
        title = "Game $id",
        thumbnail = "https://example.com/thumb$id.jpg",
        shortDescription = "Description $id",
        gameUrl = "https://example.com/game$id",
        genre = genre,
        platform = "PC (Windows)",
        publisher = "Publisher",
        developer = "Developer",
        releaseDate = "2023-01-01",
        freetogameProfileUrl = "https://example.com/profile$id"
    )

    @Test
    fun upsertAndObserveGames() = runTest {
        val games = listOf(sampleGame(1), sampleGame(2))
        dataSource.upsertGames(games)

        dataSource.observeGames().test {
            val result = awaitItem()
            assertEquals(2, result.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun observeGameByIdReturnsDetail() = runTest {
        val detail = GameDetail(
            game = sampleGame(1),
            description = "Full description",
            status = "Live",
            screenshots = listOf(Screenshot(id = 100, image = "https://example.com/ss.jpg")),
            minimumSystemRequirements = SystemRequirements(
                os = "Windows 10",
                processor = "i5",
                memory = "8GB",
                graphics = "GTX 1060",
                storage = "30GB"
            )
        )
        dataSource.upsertGameDetail(detail)

        dataSource.observeGameById(1).test {
            val result = awaitItem()
            assertNotNull(result)
            assertEquals("Full description", result.description)
            assertEquals(1, result.screenshots.size)
            assertEquals("Windows 10", result.minimumSystemRequirements?.os)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun searchByTitleFiltersCorrectly() = runTest {
        dataSource.upsertGames(listOf(
            sampleGame(1).copy(title = "Genshin Impact"),
            sampleGame(2).copy(title = "Lost Ark"),
            sampleGame(3).copy(title = "Genshin Star")
        ))

        dataSource.searchByTitle("Genshin").test {
            val result = awaitItem()
            assertEquals(2, result.size)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
