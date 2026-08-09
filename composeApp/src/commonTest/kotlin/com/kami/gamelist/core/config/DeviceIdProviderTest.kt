package com.kami.gamelist.core.config

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.kami.gamelist.data.local.TextPrefDataSource
import com.kami.gamelist.db.GameListDatabase
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DeviceIdProviderTest {

    private lateinit var dataSource: TextPrefDataSource

    @BeforeTest
    fun setup() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        GameListDatabase.Schema.create(driver)
        dataSource = TextPrefDataSource(GameListDatabase(driver))
    }

    @Test
    fun textPrefRoundTrips() {
        assertNull(dataSource.get("missing"))

        dataSource.set("k", "v")
        assertEquals("v", dataSource.get("k"))

        dataSource.set("k", "v2")
        assertEquals("v2", dataSource.get("k"))

        dataSource.delete("k")
        assertNull(dataSource.get("k"))
    }

    @Test
    fun deviceIdIsStableAcrossCalls() {
        val provider = DeviceIdProvider(dataSource)

        val first = provider.deviceId()
        val second = provider.deviceId()

        assertEquals(first, second)
    }

    @Test
    fun deviceIdSurvivesANewProviderInstance() {
        val first = DeviceIdProvider(dataSource).deviceId()

        // Simula reabrir o app: instancia nova, mesmo banco.
        val second = DeviceIdProvider(dataSource).deviceId()

        assertEquals(first, second)
    }

    @Test
    fun deviceIdFitsTheBackendLimit() {
        val id = DeviceIdProvider(dataSource).deviceId()

        // O backend rejeita device_id acima de 128 chars com 400.
        assertTrue(id.length <= 128, "device_id tem ${id.length} chars")
        assertTrue(id.isNotBlank())
    }

    @Test
    fun twoDevicesGetDifferentIds() {
        val driverB = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        GameListDatabase.Schema.create(driverB)
        val otherDataSource = TextPrefDataSource(GameListDatabase(driverB))

        val a = DeviceIdProvider(dataSource).deviceId()
        val b = DeviceIdProvider(otherDataSource).deviceId()

        assertTrue(a != b)
    }
}
