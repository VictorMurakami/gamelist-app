package com.kami.gamelist.data.local

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.kami.gamelist.db.GameListDatabase
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CacheManagerUpdateDismissalTest {

    private lateinit var driver: JdbcSqliteDriver
    private lateinit var cacheManager: CacheManager

    @BeforeTest
    fun setup() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        GameListDatabase.Schema.create(driver)
        cacheManager = CacheManager(GameListDatabase(driver))
    }

    @Test
    fun `dismissing a version suppresses it`() {
        assertFalse(cacheManager.isUpdateDismissed("2.0.0"))

        cacheManager.markUpdateDismissed("2.0.0")

        assertTrue(cacheManager.isUpdateDismissed("2.0.0"))
    }

    @Test
    fun `dismissing one version does not suppress another`() {
        cacheManager.markUpdateDismissed("2.0.0")

        assertFalse(cacheManager.isUpdateDismissed("3.0.0"))
    }

    @Test
    fun `dismissal survives a new CacheManager instance against the same database`() {
        cacheManager.markUpdateDismissed("2.0.0")

        val reopened = CacheManager(GameListDatabase(driver))

        assertTrue(reopened.isUpdateDismissed("2.0.0"))
    }
}
