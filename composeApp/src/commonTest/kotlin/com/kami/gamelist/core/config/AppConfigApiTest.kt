package com.kami.gamelist.core.config

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
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AppConfigApiTest {

    private val fullJson = """
        {
          "update": {
            "status": "forced",
            "latest_version": "1.5.0",
            "store_url": "https://play.google.com/store/apps/details?id=com.kami.gamelist",
            "changelog": "Novo sistema de listas"
          },
          "maintenance": { "active": false, "message": null },
          "flags": { "enable_social_sharing": true, "enable_cloud_sync": false },
          "auth": {
            "issuer": "http://localhost:8080/realms/gamelist",
            "client_id": "gamelist-mobile"
          }
        }
    """.trimIndent()

    private fun apiReturning(
        body: String,
        status: HttpStatusCode = HttpStatusCode.OK,
        onRequest: (String) -> Unit = {},
    ): AppConfigApi {
        val engine = MockEngine { request ->
            onRequest(request.url.toString())
            respond(
                content = body,
                status = status,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
        }
        val client = HttpClient(engine) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true; isLenient = true })
            }
        }
        return AppConfigApi(client)
    }

    @Test
    fun parsesEveryBlock() = runTest {
        val dto = apiReturning(fullJson).fetch("android", "1.0.0", "device-abc", "pt")

        assertEquals("forced", dto.update.status)
        assertEquals("1.5.0", dto.update.latestVersion)
        assertEquals("Novo sistema de listas", dto.update.changelog)
        assertEquals(false, dto.maintenance.active)
        assertNull(dto.maintenance.message)
        assertEquals(true, dto.flags["enable_social_sharing"])
        assertEquals(false, dto.flags["enable_cloud_sync"])
        assertEquals("gamelist-mobile", dto.auth.clientId)
    }

    @Test
    fun sendsEveryQueryParameter() = runTest {
        var seenUrl = ""
        apiReturning(fullJson, onRequest = { seenUrl = it })
            .fetch("ios", "2.3.1", "device-xyz", "en")

        assertTrue(seenUrl.contains("platform=ios"), seenUrl)
        assertTrue(seenUrl.contains("version=2.3.1"), seenUrl)
        assertTrue(seenUrl.contains("device_id=device-xyz"), seenUrl)
        assertTrue(seenUrl.contains("lang=en"), seenUrl)
        assertTrue(seenUrl.contains("/api/v1/app-config/"), seenUrl)
    }

    @Test
    fun handlesNullableFields() = runTest {
        val nullsJson = """
            {
              "update": {
                "status": "none",
                "latest_version": null,
                "store_url": null,
                "changelog": ""
              },
              "maintenance": { "active": false, "message": null },
              "flags": {},
              "auth": { "issuer": "", "client_id": "" }
            }
        """.trimIndent()

        val dto = apiReturning(nullsJson).fetch("android", "1.0.0", "d", "en")

        assertNull(dto.update.latestVersion)
        assertNull(dto.update.storeUrl)
        assertEquals("", dto.update.changelog)
        assertTrue(dto.flags.isEmpty())
    }

    @Test
    fun ignoresUnknownFields() = runTest {
        val futureJson = """
            {
              "update": { "status": "none", "latest_version": null, "store_url": null, "changelog": "" },
              "maintenance": { "active": false, "message": null },
              "flags": {},
              "auth": { "issuer": "", "client_id": "" },
              "something_added_in_a_later_phase": { "nested": true }
            }
        """.trimIndent()

        // Um campo novo no backend nao pode quebrar um app ja publicado.
        val dto = apiReturning(futureJson).fetch("android", "1.0.0", "d", "en")

        assertEquals("none", dto.update.status)
    }
}
