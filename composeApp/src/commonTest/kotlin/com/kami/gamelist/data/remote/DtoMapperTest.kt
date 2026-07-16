package com.kami.gamelist.data.remote

import com.kami.gamelist.data.remote.dto.GameDetailDto
import com.kami.gamelist.data.remote.dto.GameDto
import com.kami.gamelist.data.remote.dto.MinimumSystemRequirementsDto
import com.kami.gamelist.data.remote.dto.ScreenshotDto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DtoMapperTest {

    @Test
    fun gameDtoMapsToGameDomain() {
        val dto = GameDto(
            id = 1,
            title = "Genshin Impact",
            thumbnail = "https://example.com/thumb.jpg",
            shortDescription = "An open-world RPG",
            gameUrl = "https://example.com/game",
            genre = "MMORPG",
            platform = "PC (Windows)",
            publisher = "miHoYo",
            developer = "miHoYo",
            releaseDate = "2020-09-28",
            freetogameProfileUrl = "https://example.com/profile"
        )

        val game = dto.toDomain()

        assertEquals(1, game.id)
        assertEquals("Genshin Impact", game.title)
        assertEquals("MMORPG", game.genre)
        assertEquals("PC (Windows)", game.platform)
    }

    @Test
    fun gameDetailDtoMapsToGameDetailDomain() {
        val dto = GameDetailDto(
            id = 1,
            title = "Genshin Impact",
            thumbnail = "https://example.com/thumb.jpg",
            shortDescription = "An open-world RPG",
            gameUrl = "https://example.com/game",
            genre = "MMORPG",
            platform = "PC (Windows)",
            publisher = "miHoYo",
            developer = "miHoYo",
            releaseDate = "2020-09-28",
            freetogameProfileUrl = "https://example.com/profile",
            description = "Full description here",
            status = "Live",
            screenshots = listOf(
                ScreenshotDto(id = 1, image = "https://example.com/ss1.jpg")
            ),
            minimumSystemRequirements = MinimumSystemRequirementsDto(
                os = "Windows 7",
                processor = "Intel i5",
                memory = "8 GB",
                graphics = "GTX 1060",
                storage = "30 GB"
            )
        )

        val detail = dto.toDomain()

        assertEquals(1, detail.game.id)
        assertEquals("Full description here", detail.description)
        assertEquals("Live", detail.status)
        assertEquals(1, detail.screenshots.size)
        assertEquals("Windows 7", detail.minimumSystemRequirements?.os)
    }

    @Test
    fun gameDetailDtoWithNullRequirementsMapsCorrectly() {
        val dto = GameDetailDto(
            id = 2,
            title = "Browser Game",
            thumbnail = "https://example.com/thumb.jpg",
            shortDescription = "A browser game",
            gameUrl = "https://example.com/game",
            genre = "Strategy",
            platform = "Web Browser",
            publisher = "Pub",
            developer = "Dev",
            releaseDate = "2023-01-01",
            freetogameProfileUrl = "https://example.com/profile",
            description = "Browser game description",
            status = "Live",
            screenshots = emptyList(),
            minimumSystemRequirements = null
        )

        val detail = dto.toDomain()

        assertNull(detail.minimumSystemRequirements)
        assertEquals(0, detail.screenshots.size)
    }
}
