package com.kami.gamelist.data.remote

import com.kami.gamelist.data.model.Game
import com.kami.gamelist.data.model.GameDetail
import com.kami.gamelist.data.model.Screenshot
import com.kami.gamelist.data.model.SystemRequirements
import com.kami.gamelist.data.remote.dto.GameDetailDto
import com.kami.gamelist.data.remote.dto.GameDto
import com.kami.gamelist.data.remote.dto.MinimumSystemRequirementsDto
import com.kami.gamelist.data.remote.dto.ScreenshotDto

fun GameDto.toDomain(): Game = Game(
    id = id,
    title = title,
    thumbnail = thumbnail,
    shortDescription = shortDescription,
    gameUrl = gameUrl,
    genre = genre,
    platform = platform,
    publisher = publisher,
    developer = developer,
    releaseDate = releaseDate,
    freetogameProfileUrl = freetogameProfileUrl
)

fun GameDetailDto.toDomain(): GameDetail = GameDetail(
    game = Game(
        id = id,
        title = title,
        thumbnail = thumbnail,
        shortDescription = shortDescription,
        gameUrl = gameUrl,
        genre = genre,
        platform = platform,
        publisher = publisher,
        developer = developer,
        releaseDate = releaseDate,
        freetogameProfileUrl = freetogameProfileUrl
    ),
    description = description,
    status = status,
    screenshots = screenshots.map { it.toDomain() },
    minimumSystemRequirements = minimumSystemRequirements?.toDomain()
)

fun ScreenshotDto.toDomain(): Screenshot = Screenshot(
    id = id,
    image = image
)

fun MinimumSystemRequirementsDto.toDomain(): SystemRequirements = SystemRequirements(
    os = os,
    processor = processor,
    memory = memory,
    graphics = graphics,
    storage = storage
)
