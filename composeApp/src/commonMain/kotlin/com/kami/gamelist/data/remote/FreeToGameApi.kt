package com.kami.gamelist.data.remote

import com.kami.gamelist.data.remote.dto.GameDetailDto
import com.kami.gamelist.data.remote.dto.GameDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

class FreeToGameApi(private val client: HttpClient) {

    // GET /api/games?platform={p}&category={c}&sort-by={s}
    // All params optional. platform: "windows"|"browser"|"all", category: lowercase genre
    suspend fun getGames(
        platform: String? = null,
        category: String? = null,
        sortBy: SortOption? = null
    ): List<GameDto> {
        return client.get("api/games") {
            platform?.let { parameter("platform", it) }
            category?.let { parameter("category", it.lowercase()) }
            sortBy?.let { parameter("sort-by", it.apiValue) }
        }.body()
    }

    // GET /api/filter?tag={tag1.tag2.tag3}&platform={p}&sort-by={s}
    // Tags are dot-separated. Allows filtering by multiple categories at once.
    suspend fun getGamesByTags(
        tags: List<String>,
        platform: String? = null,
        sortBy: SortOption? = null
    ): List<GameDto> {
        return client.get("api/filter") {
            parameter("tag", tags.joinToString(".") { it.lowercase() })
            platform?.let { parameter("platform", it) }
            sortBy?.let { parameter("sort-by", it.apiValue) }
        }.body()
    }

    // GET /api/game?id={id}
    suspend fun getGameById(id: Int): GameDetailDto {
        return client.get("api/game") {
            parameter("id", id)
        }.body()
    }
}
