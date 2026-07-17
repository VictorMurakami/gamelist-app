package com.kami.gamelist.feature.settings

import cafe.adriel.voyager.core.model.ScreenModel
import com.kami.gamelist.data.local.CacheManager
import com.kami.gamelist.data.repository.GameRepository
import com.kami.gamelist.data.repository.UserRepository

class SettingsScreenModel(
    private val gameRepository: GameRepository,
    private val userRepository: UserRepository,
    private val cacheManager: CacheManager,
) : ScreenModel {

    fun clearGameCache() {
        gameRepository.clearCache()
    }

    fun clearSearchHistory() {
        userRepository.clearSearchHistory()
    }

    fun resetOnboarding() {
        cacheManager.resetOnboarding()
    }
}
