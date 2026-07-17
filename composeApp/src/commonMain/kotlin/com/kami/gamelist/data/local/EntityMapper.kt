package com.kami.gamelist.data.local

import com.kami.gamelist.data.model.Game
import com.kami.gamelist.data.model.GameDetail
import com.kami.gamelist.data.model.ListType
import com.kami.gamelist.data.model.Screenshot
import com.kami.gamelist.data.model.SystemRequirements
import com.kami.gamelist.data.model.UserList
import com.kami.gamelist.GameEntity
import com.kami.gamelist.ScreenshotEntity
import com.kami.gamelist.UserListEntity

fun GameEntity.toDomain(): Game = Game(
    id = id.toInt(),
    title = title,
    thumbnail = thumbnail,
    shortDescription = short_description,
    gameUrl = game_url,
    genre = genre,
    platform = platform,
    publisher = publisher,
    developer = developer,
    releaseDate = release_date,
    freetogameProfileUrl = freetogame_profile_url
)

fun GameEntity.toDetailDomain(screenshots: List<ScreenshotEntity>): GameDetail = GameDetail(
    game = toDomain(),
    description = description ?: "",
    status = status ?: "",
    screenshots = screenshots.map { it.toDomain() },
    minimumSystemRequirements = if (min_req_os != null || min_req_processor != null) {
        SystemRequirements(
            os = min_req_os,
            processor = min_req_processor,
            memory = min_req_memory,
            graphics = min_req_graphics,
            storage = min_req_storage
        )
    } else null
)

fun ScreenshotEntity.toDomain(): Screenshot = Screenshot(
    id = id.toInt(),
    image = image
)

fun UserListEntity.toDomain(): UserList = UserList(
    id = id,
    name = name,
    type = ListType.valueOf(type),
    createdAt = created_at
)
