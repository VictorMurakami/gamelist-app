package com.kami.gamelist.core.database

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.kami.gamelist.db.GameListDatabase

actual class DriverFactory {
    actual fun createDriver(): SqlDriver {
        return NativeSqliteDriver(GameListDatabase.Schema, "gamelist.db")
    }
}
