package com.kami.gamelist.feature.home

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.kami.gamelist.core.ui.UiState
import com.kami.gamelist.data.model.Game
import com.kami.gamelist.data.repository.GameRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeData(
    val games: List<Game>,
    val genres: List<String>,
    val platforms: List<String>
)

@OptIn(ExperimentalCoroutinesApi::class)
class HomeScreenModel(
    private val gameRepository: GameRepository,
    isOnlineFlow: Flow<Boolean>
) : ScreenModel {

    private val _selectedGenre = MutableStateFlow<String?>(null)
    val selectedGenre: StateFlow<String?> = _selectedGenre.asStateFlow()

    private val _selectedPlatform = MutableStateFlow<String?>(null)
    val selectedPlatform: StateFlow<String?> = _selectedPlatform.asStateFlow()

    val isOffline: StateFlow<Boolean> = isOnlineFlow
        .map { !it }
        .stateIn(screenModelScope, SharingStarted.WhileSubscribed(5000), false)

    val uiState: StateFlow<UiState<HomeData>> = combine(
        _selectedGenre,
        _selectedPlatform
    ) { genre, platform -> Pair(genre, platform) }
        .flatMapLatest { (genre, platform) ->
            combine(
                gameRepository.observeGames(genre, platform),
                gameRepository.observeGenres(),
                gameRepository.observePlatforms()
            ) { games, genres, platforms ->
                UiState.Success(HomeData(games, genres, platforms)) as UiState<HomeData>
            }
        }
        .catch { emit(UiState.Error(it.message ?: "Unknown error")) }
        .stateIn(screenModelScope, SharingStarted.WhileSubscribed(5000), UiState.Loading)

    fun selectGenre(genre: String?) {
        _selectedGenre.value = genre
    }

    fun selectPlatform(platform: String?) {
        _selectedPlatform.value = platform
    }

    fun refresh() {
        screenModelScope.launch {
            gameRepository.refreshGames()
        }
    }
}
