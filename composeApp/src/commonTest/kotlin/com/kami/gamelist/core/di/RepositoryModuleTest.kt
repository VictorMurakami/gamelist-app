package com.kami.gamelist.core.di

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.kami.gamelist.core.config.AppConfigRepository
import com.kami.gamelist.core.config.AppInfo
import com.kami.gamelist.core.config.DeviceIdProvider
import com.kami.gamelist.core.config.UpdateStatus
import com.kami.gamelist.data.local.TextPrefDataSource
import com.kami.gamelist.db.GameListDatabase
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockEngineConfig
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.qualifier.named
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

/**
 * Mirrors [NetworkModuleTest]: exercises the real, unmodified [repositoryModule]
 * -- specifically the `AppConfigRepository(get(), get(), get(), get())` single,
 * whose four positional `get()` calls must resolve `AppConfigApi`,
 * `TextPrefDataSource`, `DeviceIdProvider` and `AppInfo` in that exact order --
 * and swaps only leaf dependencies for test doubles via [org.koin.core.Koin.declare],
 * the same substitution mechanism Koin's own testing guidance recommends.
 *
 * `databaseModule`'s `DriverFactory` is platform-registered (Android/iOS entry
 * points only), so this test stands up `networkModule` + `repositoryModule` and
 * declares a hand-built `TextPrefDataSource` backed by a real in-memory
 * SQLDelight driver, plus `DeviceIdProvider` and `AppInfo`, instead of pulling
 * `databaseModule` in wholesale.
 *
 * Resolution and behaviour are checked as two separate, explicitly-labelled
 * steps. NetworkModuleTest's failure signal for a broken wiring is indirect --
 * it dies with a Ktor `JsonConvertException` before reaching the named
 * assertion, which sends the next reader hunting a JSON bug instead of a DI
 * one. Here, both `koin.get<AppConfigRepository>()` and `.load()` are wrapped
 * so any exception -- Koin's `NoDefinitionFoundException`, a `ClassCastException`,
 * anything -- surfaces as a `fail(...)` with a message that names the DI wiring
 * as the suspect, not as an unrelated stack trace.
 */
class RepositoryModuleTest {

    @AfterTest
    fun tearDown() = stopKoin()

    // Ver o comentario equivalente em AppConfigRepositoryTest.repository(): o
    // MockEngine precisa compartilhar o scheduler virtual do runTest, senao
    // o withTimeoutOrNull dentro de AppConfigRepository.load() dispara antes
    // da resposta mockada "chegar" pelo dispatcher real do engine.
    private fun TestScope.mockJsonClient(body: String): HttpClient = HttpClient(
        MockEngine(MockEngineConfig().apply {
            dispatcher = StandardTestDispatcher(testScheduler)
            addHandler {
                respond(
                    content = body,
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                )
            }
        }),
    ) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true; isLenient = true })
        }
    }

    @Test
    fun appConfigRepositoryResolvesAndBehavesCorrectlyThroughKoin() = runTest {
        val backendBody = """{"update":{"status":"forced"},"maintenance":{},"auth":{}}"""

        val koin = startKoin { modules(networkModule, repositoryModule) }.koin

        // Swap only the two leaf HttpClient bindings -- FreeToGameApi's and
        // AppConfigApi's `single` definitions in networkModule are untouched.
        koin.declare(mockJsonClient(backendBody))
        koin.declare(mockJsonClient(backendBody), qualifier = named("backend"))

        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        GameListDatabase.Schema.create(driver)
        val textPrefs = TextPrefDataSource(GameListDatabase(driver))

        koin.declare(textPrefs)
        koin.declare(DeviceIdProvider(textPrefs))
        koin.declare(AppInfo(platform = "android", version = "1.0.0"))

        val repository = runCatching { koin.get<AppConfigRepository>() }
            .getOrElse {
                fail(
                    "AppConfigRepository failed to resolve via Koin -- check " +
                        "RepositoryModule's `AppConfigRepository(get(), get(), get(), get())` " +
                        "wiring (expected order: api, textPrefs, deviceIdProvider, appInfo). " +
                        "Cause: $it",
                )
            }

        val state = runCatching { repository.load("en") }
            .getOrElse {
                fail(
                    "AppConfigRepository resolved via Koin but load() failed -- a get() slot " +
                        "likely bound the wrong dependency. Cause: $it",
                )
            }

        assertEquals(
            UpdateStatus.FORCED,
            state.update.status,
            "AppConfigRepository resolved via Koin did not decode the mocked backend " +
                "response -- check RepositoryModule's get() wiring",
        )
    }
}
