package com.kami.gamelist.data.model

data class Game(
    val id: Int,
    val title: String,
    val thumbnail: String,
    val shortDescription: String,
    val gameUrl: String,
    val genre: String,
    val platform: String,
    val publisher: String,
    val developer: String,
    val releaseDate: String,
    val freetogameProfileUrl: String
)

data class GameDetail(
    val game: Game,
    val description: String,
    val status: String,
    val screenshots: List<Screenshot>,
    val minimumSystemRequirements: SystemRequirements?
)

data class Screenshot(
    val id: Int,
    val image: String
)

data class SystemRequirements(
    val os: String?,
    val processor: String?,
    val memory: String?,
    val graphics: String?,
    val storage: String?
)
