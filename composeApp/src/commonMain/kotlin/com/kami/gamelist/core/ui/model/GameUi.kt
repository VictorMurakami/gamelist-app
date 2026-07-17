package com.kami.gamelist.core.ui.model

import com.kami.gamelist.data.model.Game
import com.kami.gamelist.data.model.GameDetail
import com.kami.gamelist.data.model.Screenshot
import com.kami.gamelist.data.model.SystemRequirements

data class GameUi(
    val id: Int,
    val title: String,
    val thumbnail: String,
    val genre: String,
    val platform: String,
)

data class GameDetailUi(
    val id: Int,
    val title: String,
    val thumbnail: String,
    val genre: String,
    val platform: String,
    val publisher: String,
    val developer: String,
    val releaseDate: String,
    val description: String,
    val status: String,
    val screenshots: List<ScreenshotUi>,
    val systemRequirements: SystemRequirementsUi?,
)

data class ScreenshotUi(
    val id: Int,
    val image: String,
)

data class SystemRequirementsUi(
    val os: String?,
    val processor: String?,
    val memory: String?,
    val graphics: String?,
    val storage: String?,
)

fun Game.toUi() = GameUi(
    id = id,
    title = title,
    thumbnail = thumbnail,
    genre = genre,
    platform = platform,
)

fun GameDetail.toUi() = GameDetailUi(
    id = game.id,
    title = game.title,
    thumbnail = game.thumbnail,
    genre = game.genre,
    platform = game.platform,
    publisher = game.publisher,
    developer = game.developer,
    releaseDate = game.releaseDate,
    description = description,
    status = status,
    screenshots = screenshots.map { it.toUi() },
    systemRequirements = minimumSystemRequirements?.toUi(),
)

fun Screenshot.toUi() = ScreenshotUi(id = id, image = image)

fun SystemRequirements.toUi() = SystemRequirementsUi(
    os = os,
    processor = processor,
    memory = memory,
    graphics = graphics,
    storage = storage,
)
