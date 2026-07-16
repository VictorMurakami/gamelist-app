package com.kami.gamelist.feature.detail

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.kami.gamelist.core.ui.UiState
import com.kami.gamelist.data.model.GameDetail
import com.kami.gamelist.data.model.UserList
import com.kami.gamelist.data.repository.GameRepository
import com.kami.gamelist.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class GameDetailScreenModel(
    private val gameId: Int,
    private val gameRepository: GameRepository,
    private val userRepository: UserRepository
) : ScreenModel {

    val uiState: StateFlow<UiState<GameDetail>> = gameRepository
        .observeGameDetail(gameId)
        .map { detail ->
            if (detail != null) UiState.Success(detail)
            else UiState.Loading
        }
        .stateIn(screenModelScope, SharingStarted.WhileSubscribed(5000), UiState.Loading)

    val isFavorite: StateFlow<Boolean> = userRepository
        .isFavorite(gameId)
        .stateIn(screenModelScope, SharingStarted.WhileSubscribed(5000), false)

    val lists: StateFlow<List<UserList>> = userRepository
        .observeLists()
        .stateIn(screenModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val listsContainingGame: StateFlow<Set<Long>> = lists
        .flatMapLatest { allLists ->
            if (allLists.isEmpty()) flowOf(emptySet())
            else {
                val flows = allLists.map { list ->
                    userRepository.isInList(list.id, gameId).map { isIn ->
                        if (isIn) list.id else null
                    }
                }
                combine(flows) { results -> results.filterNotNull().toSet() }
            }
        }
        .stateIn(screenModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    private val _showListSelector = MutableStateFlow(false)
    val showListSelector: StateFlow<Boolean> = _showListSelector.asStateFlow()

    fun retry() {
        screenModelScope.launch {
            gameRepository.refreshGameDetail(gameId)
        }
    }

    fun toggleFavorite() {
        userRepository.toggleFavorite(gameId)
    }

    fun onShowListSelector() { _showListSelector.value = true }
    fun onDismissListSelector() { _showListSelector.value = false }

    fun toggleList(list: UserList) {
        screenModelScope.launch {
            val isIn = userRepository.isInList(list.id, gameId).first()
            if (isIn) userRepository.removeFromList(list.id, gameId)
            else userRepository.addToList(list.id, gameId)
        }
    }
}
