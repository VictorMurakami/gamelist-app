package com.kami.gamelist.feature.lists

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.kami.gamelist.core.ui.model.GameUi
import com.kami.gamelist.core.ui.model.toUi
import com.kami.gamelist.data.repository.UserRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class ListDetailScreenModel(
    private val listId: Long,
    private val userRepository: UserRepository
) : ScreenModel {

    val games: StateFlow<List<GameUi>> = userRepository
        .observeGamesInList(listId)
        .map { list -> list.map { it.toUi() } }
        .stateIn(screenModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteIds: StateFlow<Set<Int>> = userRepository.observeFavorites()
        .map { list -> list.map { it.id }.toSet() }
        .stateIn(screenModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    fun removeFromList(gameId: Int) {
        userRepository.removeFromList(listId, gameId)
    }

    fun toggleFavorite(gameId: Int) {
        userRepository.toggleFavorite(gameId)
    }
}
