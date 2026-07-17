package com.kami.gamelist.feature.lists

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.kami.gamelist.core.ui.model.ListUi
import com.kami.gamelist.core.ui.model.toUi
import com.kami.gamelist.data.model.ListType
import com.kami.gamelist.data.repository.UserRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

enum class ListSortOption(val label: String) {
    NEWEST("Newest"),
    NAME_ASC("A-Z"),
    GAME_COUNT("Most Games"),
}

@OptIn(ExperimentalCoroutinesApi::class)
class ListsScreenModel(
    private val userRepository: UserRepository
) : ScreenModel {

    private val _sortOption = MutableStateFlow(ListSortOption.NEWEST)
    val sortOption: StateFlow<ListSortOption> = _sortOption.asStateFlow()

    private val unsortedLists: StateFlow<List<ListUi>> = userRepository
        .observeLists()
        .flatMapLatest { userLists ->
            if (userLists.isEmpty()) flowOf(emptyList())
            else {
                val countFlows = userLists.map { list ->
                    userRepository.listGameCount(list.id).map { count ->
                        list.toUi().copy(gameCount = count.toInt())
                    }
                }
                combine(countFlows) { it.toList() }
            }
        }
        .stateIn(screenModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val lists: StateFlow<List<ListUi>> = combine(unsortedLists, _sortOption) { items, sort ->
        when (sort) {
            ListSortOption.NEWEST -> items.sortedByDescending { it.createdAt }
            ListSortOption.NAME_ASC -> items.sortedBy { it.name.lowercase() }
            ListSortOption.GAME_COUNT -> items.sortedByDescending { it.gameCount }
        }
    }.stateIn(screenModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSortOption(option: ListSortOption) {
        _sortOption.value = option
    }

    fun createList(name: String): Long {
        return userRepository.createList(name, ListType.CUSTOM)
    }

    fun deleteList(id: Long) {
        userRepository.deleteList(id)
    }

    fun updateListName(id: Long, name: String) {
        userRepository.updateListName(id, name)
    }
}
