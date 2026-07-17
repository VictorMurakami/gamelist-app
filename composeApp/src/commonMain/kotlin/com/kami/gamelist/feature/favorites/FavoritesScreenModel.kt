package com.kami.gamelist.feature.favorites

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.kami.gamelist.core.ui.model.GameUi
import com.kami.gamelist.core.ui.model.toUi
import com.kami.gamelist.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

enum class FavoriteSortOption(val label: String) {
    RECENTLY_ADDED("Recent"),
    NAME_ASC("A-Z"),
}

class FavoritesScreenModel(
    private val userRepository: UserRepository
) : ScreenModel {

    private val _sortOption = MutableStateFlow(FavoriteSortOption.RECENTLY_ADDED)
    val sortOption: StateFlow<FavoriteSortOption> = _sortOption.asStateFlow()

    private val unsortedFavorites: StateFlow<List<GameUi>> = userRepository
        .observeFavorites()
        .map { list -> list.map { it.toUi() } }
        .stateIn(screenModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favorites: StateFlow<List<GameUi>> = combine(unsortedFavorites, _sortOption) { items, sort ->
        when (sort) {
            FavoriteSortOption.RECENTLY_ADDED -> items
            FavoriteSortOption.NAME_ASC -> items.sortedBy { it.title.lowercase() }
        }
    }.stateIn(screenModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteIds: StateFlow<Set<Int>> = userRepository.observeFavorites()
        .map { list -> list.map { it.id }.toSet() }
        .stateIn(screenModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    fun setSortOption(option: FavoriteSortOption) {
        _sortOption.value = option
    }

    fun toggleFavorite(gameId: Int) {
        userRepository.toggleFavorite(gameId)
    }
}
