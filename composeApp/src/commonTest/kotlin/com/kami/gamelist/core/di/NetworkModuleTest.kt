package com.kami.gamelist.core.di

import com.kami.gamelist.core.config.AppConfigApi
import com.kami.gamelist.data.remote.FreeToGameApi
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
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.qualifier.named
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Pins the exact hazard the Task 3 brief called the highest risk in this
 * module: `networkModule` registers two [HttpClient]s. If they were ever
 * both registered without a qualifier (or under the same one), Koin would
 * silently let the later registration win, and both [FreeToGameApi] and
 * [AppConfigApi] would resolve to the same client -- breaking one of them
 * at runtime, with no compile error and no failing test, because every
 * other test in the suite builds its API object directly with a MockEngine
 * client and never goes through Koin at all.
 *
 * This test exercises the real, unmodified [networkModule] and swaps only
 * the leaf [HttpClient] instances for instrumented MockEngine doubles via
 * [org.koin.core.Koin.declare] -- the same mechanism Koin's own testing
 * guidance uses to substitute dependencies. It never reaches into a private
 * field: the property under test (which client each API actually uses) is
 * observed behaviourally, by making each API perform its real network call
 * and recording which MockEngine received the request.
 */
class NetworkModuleTest {

    @AfterTest
    fun tearDown() {
        stopKoin()
    }

    private fun mockJsonClient(engine: MockEngine): HttpClient = HttpClient(engine) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true; isLenient = true })
        }
    }

    @Test
    fun freeToGameApiUsesDefaultClientAndAppConfigApiUsesBackendClient() = runTest {
        var defaultClientHit = false
        var backendClientHit = false

        val defaultEngine = MockEngine { _ ->
            defaultClientHit = true
            respond(
                content = "[]",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
        }
        val backendEngine = MockEngine { _ ->
            backendClientHit = true
            respond(
                content = """{"update":{"status":"none"},"maintenance":{},"auth":{}}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
        }

        val koin = startKoin { modules(networkModule) }.koin

        // Swap only the two leaf HttpClient bindings for instrumented doubles.
        // FreeToGameApi's and AppConfigApi's `single` definitions are untouched
        // and still resolve via the exact `get()` / `get(named("backend"))`
        // calls declared in networkModule.
        koin.declare(mockJsonClient(defaultEngine))
        koin.declare(mockJsonClient(backendEngine), qualifier = named("backend"))

        koin.get<FreeToGameApi>().getGames()
        assertTrue(defaultClientHit, "FreeToGameApi must resolve the unqualified (default) HttpClient")
        assertFalse(backendClientHit, "FreeToGameApi must not resolve the backend-qualified HttpClient")

        koin.get<AppConfigApi>().fetch("android", "1.0.0", "device-id", "en")
        assertTrue(backendClientHit, "AppConfigApi must resolve the backend-qualified HttpClient")
    }
}
