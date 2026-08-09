package com.kami.gamelist.core.config

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.kami.gamelist.data.local.TextPrefDataSource
import com.kami.gamelist.db.GameListDatabase
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Reproduz deterministicamente a corrida entre duas chamadas a deviceId()
 * que veem o store vazio ao mesmo tempo — sem precisar de threads reais.
 *
 * O truque: um TextPrefDataSource "de corrida" intercepta a PRIMEIRA vez que
 * seu get() enxerga o store vazio e, antes de devolver esse null para quem
 * chamou, executa por inteiro a chamada deviceId() de um segundo provider
 * sobre o mesmo banco. Isso reproduz exatamente o cenario da race: a
 * chamada A ja tinha visto "vazio" quando a chamada B roda do zero, gera um
 * valor diferente e grava primeiro.
 */
class DeviceIdConcurrencyTest {

    @Test
    fun concurrentFirstCallsAgreeOnTheSamePersistedId() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        GameListDatabase.Schema.create(driver)
        val database = GameListDatabase(driver)

        val dataSourceB = TextPrefDataSource(database)
        val providerB = DeviceIdProvider(dataSourceB)

        var raceTriggered = false
        var bResult: String? = null
        val racingDataSource = object : TextPrefDataSource(database) {
            override fun get(key: String): String? {
                val value = super.get(key)
                if (value == null && !raceTriggered) {
                    raceTriggered = true
                    // Enquanto A ainda acha o store vazio (nao escreveu nada
                    // ainda), B roda sua chamada inteira e vence a corrida.
                    bResult = providerB.deviceId()
                }
                return value
            }
        }
        val providerA = DeviceIdProvider(racingDataSource)

        val aResult = providerA.deviceId()

        assertEquals(bResult, aResult, "as duas chamadas concorrentes devem devolver o mesmo id")
        assertEquals(aResult, dataSourceB.get("device_id"), "o id devolvido deve ser o que ficou persistido")
    }
}
