package com.kami.gamelist.feature.favorites

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.kami.gamelist.data.model.Game
import com.kami.gamelist.data.repository.UserRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class FavoritesScreenModel(
    userRepository: UserRepository
) : ScreenModel {

    val favorites: StateFlow<List<Game>> = userRepository
        .observeFavorites()
        .stateIn(screenModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
