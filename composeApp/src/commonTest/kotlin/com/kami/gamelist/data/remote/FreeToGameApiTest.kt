package com.kami.gamelist.data.remote

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

class FreeToGameApiTest {

    private val gamesJson = """
        [
            {
                "id": 1,
                "title": "Genshin Impact",
                "thumbnail": "https://example.com/thumb.jpg",
                "short_description": "An open-world RPG",
                "game_url": "https://example.com/game",
                "genre": "MMORPG",
                "platform": "PC (Windows)",
                "publisher": "miHoYo",
                "developer": "miHoYo",
                "release_date": "2020-09-28",
                "freetogame_profile_url": "https://example.com/profile"
            }
        ]
    """.trimIndent()

    private val gameDetailJson = """
        {
            "id": 1,
            "title": "Genshin Impact",
            "thumbnail": "https://example.com/thumb.jpg",
            "short_description": "An open-world RPG",
            "game_url": "https://example.com/game",
            "genre": "MMORPG",
            "platform": "PC (Windows)",
            "publisher": "miHoYo",
            "developer": "miHoYo",
            "release_date": "2020-09-28",
            "freetogame_profile_url": "https://example.com/profile",
            "description": "Full description",
            "status": "Live",
            "screenshots": [{"id": 1, "image": "https://example.com/ss1.jpg"}],
            "minimum_system_requirements": {
                "os": "Windows 7",
                "processor": "Intel i5",
                "memory": "8 GB",
                "graphics": "GTX 1060",
                "storage": "30 GB"
            }
        }
    """.trimIndent()

    private fun createMockClient(responseBody: String): HttpClient {
        return HttpClient(MockEngine) {
            engine {
                addHandler { request ->
                    respond(
                        content = responseBody,
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    )
                }
            }
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }
    }

    @Test
    fun getGamesReturnsListOfGameDto() = runTest {
        val api = FreeToGameApi(createMockClient(gamesJson))
        val games = api.getGames()

        assertEquals(1, games.size)
        assertEquals("Genshin Impact", games[0].title)
        assertEquals("MMORPG", games[0].genre)
    }

    @Test
    fun getGamesWithFilters() = runTest {
        val api = FreeToGameApi(createMockClient(gamesJson))
        val games = api.getGames(
            platform = "windows",
            category = "mmorpg",
            sortBy = SortOption.POPULARITY
        )

        assertEquals(1, games.size)
    }

    @Test
    fun getGamesByTagsReturnsList() = runTest {
        val api = FreeToGameApi(createMockClient(gamesJson))
        val games = api.getGamesByTags(
            tags = listOf("mmorpg", "open-world"),
            platform = "windows"
        )

        assertEquals(1, games.size)
    }

    @Test
    fun getGameByIdReturnsGameDetailDto() = runTest {
        val api = FreeToGameApi(createMockClient(gameDetailJson))
        val detail = api.getGameById(1)

        assertEquals(1, detail.id)
        assertEquals("Full description", detail.description)
        assertEquals(1, detail.screenshots.size)
        assertEquals("Windows 7", detail.minimumSystemRequirements?.os)
    }
}
