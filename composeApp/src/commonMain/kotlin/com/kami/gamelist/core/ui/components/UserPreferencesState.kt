package com.kami.gamelist.core.ui.components

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import com.kami.gamelist.core.ui.model.PlatformPreference
import com.kami.gamelist.data.local.CacheManager

@Stable
class UserPreferencesState(private val cacheManager: CacheManager) {

    companion object {
        private const val KEY_GENRES = "pref_user_genres"
        private const val KEY_PLATFORM = "pref_user_platform"

        val AVAILABLE_GENRES = listOf(
            "Shooter", "MMORPG", "Strategy", "MOBA",
            "Battle Royale", "Racing", "Sports", "Card Game",
            "Fighting", "Survival", "Sandbox", "Action RPG"
        )
    }

    var selectedGenres by mutableStateOf(
        bitmaskToGenres(cacheManager.getPreference(KEY_GENRES, 0))
    )
        private set

    var platformPreference by mutableStateOf(
        PlatformPreference.entries.getOrElse(
            cacheManager.getPreference(KEY_PLATFORM, 0)
        ) { PlatformPreference.ALL }
    )
        private set

    val hasPreferences: Boolean
        get() = selectedGenres.isNotEmpty()

    fun toggleGenre(genre: String) {
        selectedGenres = if (genre in selectedGenres) selectedGenres - genre else selectedGenres + genre
        cacheManager.setPreference(KEY_GENRES, genresToBitmask(selectedGenres))
    }

    fun updatePlatform(platform: PlatformPreference) {
        platformPreference = platform
        cacheManager.setPreference(KEY_PLATFORM, platform.ordinal)
    }
}

private fun genresToBitmask(genres: Set<String>): Int {
    var mask = 0
    UserPreferencesState.AVAILABLE_GENRES.forEachIndexed { index, genre ->
        if (genre in genres) mask = mask or (1 shl index)
    }
    return mask
}

private fun bitmaskToGenres(mask: Int): Set<String> {
    return UserPreferencesState.AVAILABLE_GENRES
        .filterIndexed { index, _ -> mask and (1 shl index) != 0 }
        .toSet()
}

val LocalUserPreferences = staticCompositionLocalOf<UserPreferencesState> {
    error("No UserPreferencesState provided")
}
