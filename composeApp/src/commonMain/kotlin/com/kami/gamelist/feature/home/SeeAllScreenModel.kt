package com.kami.gamelist.feature.home

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.kami.gamelist.core.ui.model.GameUi
import com.kami.gamelist.core.ui.model.toUi
import com.kami.gamelist.data.repository.GameRepository
import com.kami.gamelist.data.repository.UserRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class SeeAllScreenModel(
    section: HomeSection,
    genres: List<String>,
    gameRepository: GameRepository,
    private val userRepository: UserRepository,
) : ScreenModel {

    val games: StateFlow<List<GameUi>> = when (section) {
        HomeSection.RECENT -> gameRepository.observeRecentReleases(100)
        HomeSection.POPULAR -> gameRepository.observePopularGames()
        HomeSection.RECOMMENDED -> gameRepository.observeRecommendedGames(genres, 100)
    }.map { list -> list.map { it.toUi() } }
        .stateIn(screenModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteIds: StateFlow<Set<Int>> = userRepository.observeFavorites()
        .map { list -> list.map { it.id }.toSet() }
        .stateIn(screenModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    fun toggleFavorite(gameId: Int) {
        userRepository.toggleFavorite(gameId)
    }
}
