package com.kami.gamelist.feature.lists

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.kami.gamelist.data.model.Game
import com.kami.gamelist.data.repository.UserRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class ListDetailScreenModel(
    private val listId: Long,
    private val userRepository: UserRepository
) : ScreenModel {

    val games: StateFlow<List<Game>> = userRepository
        .observeGamesInList(listId)
        .stateIn(screenModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun removeFromList(gameId: Int) {
        userRepository.removeFromList(listId, gameId)
    }
}
