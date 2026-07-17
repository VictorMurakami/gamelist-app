package com.kami.gamelist.feature.search

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.kami.gamelist.core.ui.model.GameUi
import com.kami.gamelist.core.ui.model.SearchHistoryUi
import com.kami.gamelist.core.ui.model.toUi
import com.kami.gamelist.data.repository.GameRepository
import com.kami.gamelist.data.repository.UserRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class SearchScreenModel(
    private val gameRepository: GameRepository,
    private val userRepository: UserRepository
) : ScreenModel {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _showHistory = MutableStateFlow(true)
    val showHistory: StateFlow<Boolean> = _showHistory.asStateFlow()

    val searchResults: StateFlow<List<GameUi>> = _query
        .debounce(300)
        .flatMapLatest { q ->
            if (q.isBlank()) flowOf(emptyList())
            else gameRepository.searchGames(q).map { games -> games.map { it.toUi() } }
        }
        .stateIn(screenModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentSearches: StateFlow<List<SearchHistoryUi>> = userRepository
        .observeRecentSearches()
        .map { list -> list.map { it.toUi() } }
        .stateIn(screenModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteIds: StateFlow<Set<Int>> = userRepository.observeFavorites()
        .map { list -> list.map { it.id }.toSet() }
        .stateIn(screenModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    fun onQueryChange(newQuery: String) {
        _query.value = newQuery
        _showHistory.value = newQuery.isBlank()
    }

    fun onSearch(searchQuery: String) {
        if (searchQuery.isBlank()) return
        _query.value = searchQuery
        _showHistory.value = false
        userRepository.addSearchQuery(searchQuery)
    }

    fun onHistoryItemClick(searchQuery: String) {
        onSearch(searchQuery)
    }

    fun onHistoryItemDelete(searchQuery: String) {
        userRepository.deleteSearchQuery(searchQuery)
    }

    fun toggleFavorite(gameId: Int) {
        userRepository.toggleFavorite(gameId)
    }
}
