package com.kami.gamelist.core.config

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
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

    // Extensao de TestScope: o MockEngine precisa rodar no mesmo dispatcher
    // virtual do runTest (StandardTestDispatcher(testScheduler)), senao a
    // resposta mockada chega por um dispatcher real que o testScheduler nao
    // enxerga -- ele acha a fila ociosa e adianta o relogio virtual, disparando
    // o withTimeoutOrNull de AppConfigRepository.load() antes da resposta
    // "chegar". Compartilhar o scheduler mantem os dois no mesmo relogio.
    private fun TestScope.repository(
        body: String? = null,
        status: HttpStatusCode = HttpStatusCode.OK,
        failWith: Throwable? = null,
        delayMs: Long? = null,
        version: String = appInfo.version,
    ): AppConfigRepository {
        val engine = MockEngine(MockEngineConfig().apply {
            dispatcher = StandardTestDispatcher(testScheduler)
            addHandler {
                delayMs?.let { delay(it) }
                if (failWith != null) throw failWith
                respond(
                    content = body.orEmpty(),
                    status = status,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                )
            }
        })
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
            appInfo = appInfo.copy(version = version),
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
    fun timesOutAndFallsBackToCacheWhenBackendHangs() = runTest {
        // Cache primed from a healthy fetch, exactly like a previously-working
        // app before the backend goes dark.
        repository(json("forced")).load("en")

        // Backend accepts the connection but hangs well past FETCH_TIMEOUT_MS
        // (2_500ms in AppConfigRepository) -- rides runTest's virtual clock,
        // no real 60s wait. The body below must never actually be delivered.
        val state = repository(body = json("none"), delayMs = 60_000).load("en")

        // Falls back to the cached FORCED state, not the hung "none" response --
        // proves the cache read stays outside withTimeoutOrNull's cancelled scope.
        assertEquals(UpdateStatus.FORCED, state.update.status)
    }

    @Test
    fun timesOutAndReturnsEmptyStateWhenBackendHangsWithNoCache() = runTest {
        // Same hang, but no prior cache to fall back to.
        val state = repository(body = json("forced"), delayMs = 60_000).load("en")

        assertEquals(UpdateStatus.NONE, state.update.status)
        assertFalse(state.maintenance.active)
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
        // Corpo valido e totalmente decodificavel (status "forced") de proposito:
        // se o cliente nao rejeitasse o 400 antes de decodificar, o teste passaria
        // pelo motivo errado. Ver expectSuccess no helper `repository()`.
        val state = repository(body = json("forced"), status = HttpStatusCode.BadRequest).load("en")

        assertEquals(UpdateStatus.NONE, state.update.status)
    }

    @Test
    fun returnsEmptyStateOnThrottle() = runTest {
        // 429 e condicao rotineira: 120 req/min por IP, e um CGNAT compartilha IP.
        // Corpo valido de proposito -- ver comentario em returnsEmptyStateOnBadRequest.
        val state = repository(body = json("forced"), status = HttpStatusCode.TooManyRequests).load("en")

        assertEquals(UpdateStatus.NONE, state.update.status)
    }

    @Test
    fun returnsEmptyStateWhenUpdateBlockMissing() = runTest {
        // Falha no nivel de schema (bloco ausente), diferente de JSON malformado:
        // um deploy de backend que remove um campo, ou uma resposta truncada.
        val body = """{ "maintenance": {"active": false}, "auth": {"issuer": "i", "client_id": "c"} }"""

        val state = repository(body = body).load("en")

        assertEquals(UpdateStatus.NONE, state.update.status)
    }

    @Test
    fun returnsEmptyStateWhenMaintenanceBlockMissing() = runTest {
        // "status": "forced" e deliberado: se maintenance virasse opcional com
        // default, FORCED vazaria e este teste pegaria.
        val body = """{ "update": {"status": "forced"}, "auth": {"issuer": "i", "client_id": "c"} }"""

        val state = repository(body = body).load("en")

        assertEquals(UpdateStatus.NONE, state.update.status)
    }

    @Test
    fun returnsEmptyStateWhenAuthBlockMissing() = runTest {
        // "status": "forced" e deliberado: se auth virasse opcional com
        // default, FORCED vazaria e este teste pegaria.
        val body = """{ "update": {"status": "forced"}, "maintenance": {"active": false} }"""

        val state = repository(body = body).load("en")

        assertEquals(UpdateStatus.NONE, state.update.status)
    }

    @Test
    fun discardsCacheFromAnotherAppVersion() = runTest {
        // Sequencia real: em 1.0.0 o backend responde forced e o app cacheia.
        // O usuario faz o que a tela mandou e atualiza para 2.0.0. Na primeira
        // abertura depois disso ele esta sem rede (metro, aviao, roaming).
        repository(json("forced"), version = "1.0.0").load("en")

        val state = repository(failWith = RuntimeException("offline"), version = "2.0.0").load("en")

        // Sem esta checagem o app reaplica o forced de 1.0.0 e tranca um
        // usuario ja atualizado numa tela sem saida, cujo unico botao leva a
        // uma loja onde nao ha o que baixar. Sem rede, so a proxima conexao
        // resolveria.
        assertEquals(UpdateStatus.NONE, state.update.status)
    }

    @Test
    fun keepsCacheForTheSameAppVersion() = runTest {
        // Contraprova do teste acima: o descarte e por versao diferente, nao um
        // "nunca usa o cache" que tornaria o fallback offline inutil.
        repository(json("forced"), version = "1.0.0").load("en")

        val state = repository(failWith = RuntimeException("offline"), version = "1.0.0").load("en")

        assertEquals(UpdateStatus.FORCED, state.update.status)
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
