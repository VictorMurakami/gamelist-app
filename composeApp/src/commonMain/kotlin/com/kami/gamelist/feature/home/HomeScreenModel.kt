package com.kami.gamelist.feature.home

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.kami.gamelist.core.ui.UiState
import com.kami.gamelist.core.ui.model.GameUi
import com.kami.gamelist.core.ui.model.toUi
import com.kami.gamelist.data.remote.SortOption
import com.kami.gamelist.data.repository.GameRepository
import com.kami.gamelist.data.repository.SyncState
import com.kami.gamelist.data.repository.UserRepository
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
    val games: List<GameUi>,
    val genres: List<String>,
    val platforms: List<String>
)

@OptIn(ExperimentalCoroutinesApi::class)
class HomeScreenModel(
    private val gameRepository: GameRepository,
    private val userRepository: UserRepository,
    isOnlineFlow: Flow<Boolean>
) : ScreenModel {

    private val _selectedGenres = MutableStateFlow<Set<String>>(emptySet())
    val selectedGenres: StateFlow<Set<String>> = _selectedGenres.asStateFlow()

    private val _selectedPlatform = MutableStateFlow<String?>(null)
    val selectedPlatform: StateFlow<String?> = _selectedPlatform.asStateFlow()

    private val _sortOption = MutableStateFlow(SortOption.RELEVANCE)
    val sortOption: StateFlow<SortOption> = _sortOption.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _preferredGenres = MutableStateFlow<List<String>>(emptyList())

    val isFiltering: StateFlow<Boolean> = combine(
        _selectedGenres, _selectedPlatform
    ) { genres, platform -> genres.isNotEmpty() || platform != null }
        .stateIn(screenModelScope, SharingStarted.WhileSubscribed(5000), false)

    val syncState: StateFlow<SyncState> = gameRepository.syncState
        .stateIn(screenModelScope, SharingStarted.WhileSubscribed(5000), SyncState.Idle)

    val isOffline: StateFlow<Boolean> = isOnlineFlow
        .map { !it }
        .stateIn(screenModelScope, SharingStarted.WhileSubscribed(5000), false)

    val favoriteIds: StateFlow<Set<Int>> = userRepository.observeFavorites()
        .map { list -> list.map { it.id }.toSet() }
        .stateIn(screenModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val uiState: StateFlow<UiState<HomeData>> = combine(
        _selectedGenres,
        _selectedPlatform,
        _sortOption
    ) { genres, platform, sort -> Triple(genres, platform, sort) }
        .flatMapLatest { (genres, platform, sort) ->
            combine(
                gameRepository.observeGamesFiltered(genres, platform),
                gameRepository.observeGenres(),
                gameRepository.observePlatforms()
            ) { games, genreList, platforms ->
                val sorted = when (sort) {
                    SortOption.RELEASE_DATE -> games.sortedByDescending { it.releaseDate }
                    SortOption.ALPHABETICAL -> games.sortedBy { it.title }
                    else -> games
                }
                UiState.Success(
                    HomeData(
                        games = sorted.map { it.toUi() },
                        genres = genreList,
                        platforms = platforms
                    )
                ) as UiState<HomeData>
            }
        }
        .catch { emit(UiState.Error(it.message ?: "Unknown error")) }
        .stateIn(screenModelScope, SharingStarted.WhileSubscribed(5000), UiState.Loading)

    val recentReleases: StateFlow<List<GameUi>> = gameRepository.observeRecentReleases(20)
        .map { list -> list.map { it.toUi() } }
        .stateIn(screenModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val popularGames: StateFlow<List<GameUi>> = gameRepository.observePopularGames()
        .map { list -> list.map { it.toUi() } }
        .stateIn(screenModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recommendedGames: StateFlow<List<GameUi>> = _preferredGenres
        .flatMapLatest { genres -> gameRepository.observeRecommendedGames(genres, 20) }
        .map { list -> list.map { it.toUi() } }
        .stateIn(screenModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        screenModelScope.launch { gameRepository.syncPopularGames() }
    }

    fun setPreferredGenres(genres: List<String>) {
        _preferredGenres.value = genres
    }

    fun toggleGenre(genre: String) {
        _selectedGenres.value = _selectedGenres.value.let {
            if (genre in it) it - genre else it + genre
        }
    }

    fun clearGenres() {
        _selectedGenres.value = emptySet()
    }

    fun selectPlatform(platform: String?) {
        _selectedPlatform.value = platform
    }

    fun setSortOption(sort: SortOption) {
        _sortOption.value = sort
    }

    fun refresh() {
        screenModelScope.launch {
            _isRefreshing.value = true
            gameRepository.refreshGames()
            gameRepository.syncPopularGames()
            _isRefreshing.value = false
        }
    }

    fun toggleFavorite(gameId: Int) {
        userRepository.toggleFavorite(gameId)
    }
}
