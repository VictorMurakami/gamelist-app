package com.kami.gamelist.feature.lists

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.kami.gamelist.data.model.ListType
import com.kami.gamelist.data.model.UserList
import com.kami.gamelist.data.repository.UserRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class ListsScreenModel(
    private val userRepository: UserRepository
) : ScreenModel {

    val lists: StateFlow<List<UserList>> = userRepository
        .observeLists()
        .stateIn(screenModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun createList(name: String, type: ListType): Long {
        return userRepository.createList(name, type)
    }

    fun deleteList(id: Long) {
        userRepository.deleteList(id)
    }

    fun updateListName(id: Long, name: String) {
        userRepository.updateListName(id, name)
    }
}
