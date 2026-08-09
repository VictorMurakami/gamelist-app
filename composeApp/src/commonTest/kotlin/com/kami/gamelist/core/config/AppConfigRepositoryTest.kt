package com.kami.gamelist.core.config

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.kami.gamelist.data.local.TextPrefDataSource
import com.kami.gamelist.db.GameListDatabase
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AppConfigRepositoryTest {

    private lateinit var textPrefs: TextPrefDataSource

    private val appInfo = AppInfo(platform = "android", version = "1.0.0")

    @BeforeTest
    fun setup() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        GameListDatabase.Schema.create(driver)
        textPrefs = TextPrefDataSource(GameListDatabase(driver))
    }

    private fun json(status: String, maintenanceActive: Boolean = false) = """
        {
          "update": {
            "status": "$status",
            "latest_version": "1.5.0",
            "store_url": "https://play.google.com/store/apps/details?id=com.kami.gamelist",
            "changelog": "Notas"
          },
          "maintenance": { "active": $maintenanceActive, "message": "Voltamos ja" },
          "flags": { "enable_sync": true, "enable_sharing": false },
          "auth": { "issuer": "http://kc/realms/gamelist", "client_id": "gamelist-mobile" }
        }
    """.trimIndent()

    private fun repository(
        body: String? = null,
        status: HttpStatusCode = HttpStatusCode.OK,
        failWith: Throwable? = null,
    ): AppConfigRepository {
        val engine = MockEngine {
            if (failWith != null) throw failWith
            respond(
                content = body.orEmpty(),
                status = status,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
        }
        val client = HttpClient(engine) {
            // Igual ao createBackend: 400 e 429 precisam virar excecao aqui,
            // senao os testes de erro passam pelo motivo errado.
            expectSuccess = true
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true; isLenient = true })
            }
        }
        return AppConfigRepository(
            api = AppConfigApi(client),
            textPrefs = textPrefs,
            deviceIdProvider = DeviceIdProvider(textPrefs),
            appInfo = appInfo,
        )
    }

    @Test
    fun mapsStatusToEnum() = runTest {
        assertEquals(UpdateStatus.FORCED, repository(json("forced")).load("en").update.status)
        assertEquals(UpdateStatus.RECOMMENDED, repository(json("recommended")).load("en").update.status)
        assertEquals(UpdateStatus.NONE, repository(json("none")).load("en").update.status)
    }

    @Test
    fun unknownStatusFallsBackToNone() = runTest {
        // Um status novo no backend nao pode bloquear um app antigo.
        val state = repository(json("something_new")).load("en")

        assertEquals(UpdateStatus.NONE, state.update.status)
    }

    @Test
    fun mapsEveryBlock() = runTest {
        val state = repository(json("recommended", maintenanceActive = true)).load("en")

        assertEquals("1.5.0", state.update.latestVersion)
        assertEquals("Notas", state.update.changelog)
        assertTrue(state.maintenance.active)
        assertEquals("Voltamos ja", state.maintenance.message)
        assertEquals(true, state.flags["enable_sync"])
        assertEquals("gamelist-mobile", state.clientId)
    }

    @Test
    fun cachesTheResponse() = runTest {
        repository(json("forced")).load("en")

        val cached = textPrefs.get("app_config_cache")

        assertTrue(cached != null && cached.contains("forced"))
    }

    @Test
    fun servesCacheWhenTheNetworkFails() = runTest {
        repository(json("forced")).load("en")

        // Agora a rede cai. O app precisa continuar sabendo que esta bloqueado.
        val state = repository(failWith = RuntimeException("network down")).load("en")

        assertEquals(UpdateStatus.FORCED, state.update.status)
    }

    @Test
    fun returnsEmptyStateWhenNetworkFailsWithNoCache() = runTest {
        val state = repository(failWith = RuntimeException("network down")).load("en")

        // Sem informacao confiavel, o app abre normalmente.
        assertEquals(UpdateStatus.NONE, state.update.status)
        assertFalse(state.maintenance.active)
        assertTrue(state.flags.isEmpty())
    }

    @Test
    fun returnsEmptyStateOnBadRequest() = runTest {
        val state = repository(body = "{}", status = HttpStatusCode.BadRequest).load("en")

        assertEquals(UpdateStatus.NONE, state.update.status)
    }

    @Test
    fun returnsEmptyStateOnThrottle() = runTest {
        // 429 e condicao rotineira: 120 req/min por IP, e um CGNAT compartilha IP.
        val state = repository(body = "{}", status = HttpStatusCode.TooManyRequests).load("en")

        assertEquals(UpdateStatus.NONE, state.update.status)
    }

    @Test
    fun corruptCacheDoesNotCrash() = runTest {
        textPrefs.set("app_config_cache", "{ not json at all")

        val state = repository(failWith = RuntimeException("network down")).load("en")

        assertEquals(UpdateStatus.NONE, state.update.status)
    }

    @Test
    fun freshResponseReplacesStaleCache() = runTest {
        repository(json("forced")).load("en")

        val state = repository(json("none")).load("en")

        assertEquals(UpdateStatus.NONE, state.update.status)
        assertEquals(UpdateStatus.NONE, repository(failWith = RuntimeException("down")).load("en").update.status)
    }

    @Test
    fun emptyStateHasNoStoreUrl() {
        assertNull(AppConfigState.EMPTY.update.storeUrl)
        assertEquals(UpdateStatus.NONE, AppConfigState.EMPTY.update.status)
    }

    @Test
    fun featureFlagsReadFromState() = runTest {
        val flags = FeatureFlags(repository(json("none")).load("en"))

        assertTrue(flags.isEnabled("enable_sync"))
        assertFalse(flags.isEnabled("enable_sharing"))
        // Flag desconhecida e sempre desligada — nunca ligar algo por engano.
        assertFalse(flags.isEnabled("never_heard_of_this"))
    }
}
