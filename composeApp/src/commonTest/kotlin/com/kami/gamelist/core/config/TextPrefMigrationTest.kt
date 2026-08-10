package com.kami.gamelist.core.config

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.kami.gamelist.data.local.TextPrefDataSource
import com.kami.gamelist.db.GameListDatabase
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Prova que a migracao 1 -> 2 (arquivo `1.sqm`) cria TextPrefEntity em um
 * banco que ja existia na v1, sem apagar dados de tabelas pre-existentes.
 *
 * Um banco v1 tem exatamente as mesmas tabelas do schema atual (v2) menos
 * TextPrefEntity: `1.sqm` e a unica migracao existente e o `migrateInternal`
 * gerado so age quando oldVersion <= 1 && newVersion > 1, criando apenas essa
 * tabela. Por isso simular a v1 = criar o schema atual e derrubar
 * TextPrefEntity reproduz fielmente o estado real de um device que instalou
 * o app antes desta mudanca.
 */
class TextPrefMigrationTest {

    @Test
    fun migrationFromV1CreatesTextPrefAndKeepsExistingData() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)

        // 1. Cria o schema atual (v2) e remove TextPrefEntity para simular
        //    um banco que ainda esta na v1.
        GameListDatabase.Schema.create(driver)
        driver.execute(null, "DROP TABLE TextPrefEntity", 0)

        val database = GameListDatabase(driver)

        // 2. Insere dado em tabela pre-existente, para provar que a migracao
        //    nao destroi dados ja gravados.
        database.cacheMetaQueries.upsert("games_list", 123456789L)

        // 3. Roda a migracao real gerada pelo SQLDelight, de v1 para v2.
        assertEquals(2L, GameListDatabase.Schema.version)
        GameListDatabase.Schema.migrate(driver, oldVersion = 1, newVersion = 2)

        // 4. TextPrefEntity agora existe e funciona.
        val textPrefs = TextPrefDataSource(database)
        assertEquals(null, textPrefs.get("device_id"))
        textPrefs.set("device_id", "abc123")
        assertEquals("abc123", textPrefs.get("device_id"))

        // 5. O dado gravado antes da migracao sobreviveu.
        assertEquals(123456789L, database.cacheMetaQueries.getLastFetched("games_list").executeAsOneOrNull())
    }
}
