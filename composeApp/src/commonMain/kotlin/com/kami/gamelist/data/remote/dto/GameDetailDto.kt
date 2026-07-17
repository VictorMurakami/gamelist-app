package com.kami.gamelist.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GameDetailDto(
    val id: Int,
    val title: String,
    val thumbnail: String,
    @SerialName("short_description") val shortDescription: String,
    @SerialName("game_url") val gameUrl: String,
    val genre: String,
    val platform: String,
    val publisher: String,
    val developer: String,
    @SerialName("release_date") val releaseDate: String,
    @SerialName("freetogame_profile_url") val freetogameProfileUrl: String,
    val description: String,
    val status: String,
    val screenshots: List<ScreenshotDto>,
    @SerialName("minimum_system_requirements") val minimumSystemRequirements: MinimumSystemRequirementsDto?
)

@Serializable
data class ScreenshotDto(
    val id: Int,
    val image: String
)

@Serializable
data class MinimumSystemRequirementsDto(
    val os: String? = null,
    val processor: String? = null,
    val memory: String? = null,
    val graphics: String? = null,
    val storage: String? = null
)
