package com.kami.gamelist.feature.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.kami.gamelist.core.ui.UiState
import com.kami.gamelist.core.ui.components.EmptyState
import com.kami.gamelist.core.ui.components.ErrorState
import com.kami.gamelist.core.ui.components.GameChipRow
import com.kami.gamelist.core.ui.components.GameCompactRow
import com.kami.gamelist.core.ui.components.GameGrid
import com.kami.gamelist.core.ui.components.GameGridSkeleton
import com.kami.gamelist.core.ui.components.GameHorizontalRow
import com.kami.gamelist.core.ui.components.GameMultiSelectChipRow
import com.kami.gamelist.core.ui.components.GameRankedRow
import com.kami.gamelist.core.ui.components.GameSpotlightRow
import com.kami.gamelist.core.ui.components.GameToastType
import com.kami.gamelist.core.ui.components.LocalGameToastState
import com.kami.gamelist.core.ui.components.LocalScrollToTop
import com.kami.gamelist.core.ui.components.LocalUserPreferences
import com.kami.gamelist.core.ui.components.OfflineBanner
import com.kami.gamelist.data.remote.SortOption
import com.kami.gamelist.data.repository.SyncState
import com.kami.gamelist.feature.detail.GameDetailNavScreen
import com.kami.gamelist.core.ui.localization.LocalStrings
import com.kami.gamelist.core.ui.theme.GameTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(screenModel: HomeScreenModel) {
    val navigator = LocalNavigator.currentOrThrow
    val uiState by screenModel.uiState.collectAsState()
    val selectedGenres by screenModel.selectedGenres.collectAsState()
    val selectedPlatform by screenModel.selectedPlatform.collectAsState()
    val selectedSort by screenModel.sortOption.collectAsState()
    val isFiltering by screenModel.isFiltering.collectAsState()
    val isOffline by screenModel.isOffline.collectAsState()
    val isRefreshing by screenModel.isRefreshing.collectAsState()
    val syncState by screenModel.syncState.collectAsState()
    val favoriteIds by screenModel.favoriteIds.collectAsState()
    val recentReleases by screenModel.recentReleases.collectAsState()
    val popularGames by screenModel.popularGames.collectAsState()
    val recommendedGames by screenModel.recommendedGames.collectAsState()
    val toastState = LocalGameToastState.current
    val strings = LocalStrings.current
    val colors = GameTheme.colors
    val preferences = LocalUserPreferences.current
    val scrollToTop = LocalScrollToTop.current
    val listState = rememberLazyListState()

    LaunchedEffect(preferences.selectedGenres) {
        screenModel.setPreferredGenres(preferences.selectedGenres.toList())
    }

    LaunchedEffect(scrollToTop.trigger) {
        if (scrollToTop.trigger > 0) {
            listState.animateScrollToItem(0)
        }
    }

    LaunchedEffect(syncState) {
        if (syncState is SyncState.SyncFailed) {
            toastState.show(
                (syncState as SyncState.SyncFailed).message,
                GameToastType.ERROR
            )
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        OfflineBanner(isOffline = isOffline)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(top = 16.dp, bottom = 4.dp)
        ) {
            Text(
                text = strings.appTitle,
                style = GameTheme.typography.headlineMedium,
                color = colors.accent
            )
            val gameCount = (uiState as? UiState.Success)?.data?.games?.size
            Text(
                text = if (gameCount != null) strings.freeToPlayCount(gameCount) else strings.freeCatalog,
                style = GameTheme.typography.bodySmall,
                color = colors.textMuted
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        when (val state = uiState) {
            is UiState.Loading -> {
                GameGridSkeleton(modifier = Modifier.fillMaxSize())
            }

            is UiState.Error -> {
                ErrorState(
                    message = state.message,
                    onRetry = { screenModel.refresh() }
                )
            }

            is UiState.Success -> {
                val data = state.data

                if (data.genres.isNotEmpty()) {
                    Text(
                        text = strings.genre,
                        style = GameTheme.typography.labelSmall,
                        color = colors.textMuted,
                        modifier = Modifier.padding(start = 16.dp, bottom = 4.dp)
                    )
                    GameMultiSelectChipRow(
                        options = data.genres,
                        selectedOptions = selectedGenres,
                        onOptionToggled = { screenModel.toggleGenre(it) },
                        onClearAll = { screenModel.clearGenres() },
                        allLabel = strings.all
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                if (data.platforms.isNotEmpty()) {
                    Text(
                        text = strings.platform,
                        style = GameTheme.typography.labelSmall,
                        color = colors.textMuted,
                        modifier = Modifier.padding(start = 16.dp, bottom = 4.dp)
                    )
                    GameChipRow(
                        options = data.platforms,
                        selectedOption = selectedPlatform,
                        onOptionSelected = { screenModel.selectPlatform(it) },
                        allLabel = strings.all
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                if (isFiltering) {
                    Text(
                        text = strings.sort,
                        style = GameTheme.typography.labelSmall,
                        color = colors.textMuted,
                        modifier = Modifier.padding(start = 16.dp, bottom = 4.dp)
                    )
                    GameChipRow(
                        options = listOf(strings.releaseDate, strings.alphabetical),
                        selectedOption = when (selectedSort) {
                            SortOption.RELEASE_DATE -> strings.releaseDate
                            SortOption.ALPHABETICAL -> strings.alphabetical
                            else -> null
                        },
                        onOptionSelected = { label ->
                            val sort = when (label) {
                                strings.releaseDate -> SortOption.RELEASE_DATE
                                strings.alphabetical -> SortOption.ALPHABETICAL
                                else -> SortOption.RELEVANCE
                            }
                            screenModel.setSortOption(sort)
                        },
                        allLabel = strings.defaultSort
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                Spacer(modifier = Modifier.height(4.dp))

                if (data.games.isEmpty()) {
                    EmptyState(
                        icon = Icons.Outlined.SportsEsports,
                        title = strings.noGamesFound,
                        subtitle = strings.tryAdjustingFilters
                    )
                } else if (isFiltering) {
                    PullToRefreshBox(
                        isRefreshing = isRefreshing,
                        onRefresh = { screenModel.refresh() },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        GameGrid(
                            games = data.games,
                            onGameClick = { game -> navigator.push(GameDetailNavScreen(game.id)) },
                            favoriteIds = favoriteIds,
                            onToggleFavorite = { gameId ->
                                handleFavoriteToggle(gameId, favoriteIds, screenModel, toastState, strings)
                            }
                        )
                    }
                } else {
                    PullToRefreshBox(
                        isRefreshing = isRefreshing,
                        onRefresh = { screenModel.refresh() },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        LazyColumn(state = listState) {
                            if (recentReleases.isNotEmpty()) {
                                item(key = "section_recent") {
                                    GameSpotlightRow(
                                        title = strings.recentReleases,
                                        games = recentReleases,
                                        onGameClick = { navigator.push(GameDetailNavScreen(it.id)) },
                                        onSeeAllClick = { navigator.push(SeeAllNavScreen(HomeSection.RECENT)) },
                                        favoriteIds = favoriteIds,
                                        onToggleFavorite = { gameId ->
                                            handleFavoriteToggle(gameId, favoriteIds, screenModel, toastState, strings)
                                        },
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                    Spacer(modifier = Modifier.height(24.dp))
                                }
                            }

                            if (popularGames.isNotEmpty()) {
                                item(key = "section_popular") {
                                    GameRankedRow(
                                        title = strings.popularNow,
                                        games = popularGames,
                                        onGameClick = { navigator.push(GameDetailNavScreen(it.id)) },
                                        onSeeAllClick = { navigator.push(SeeAllNavScreen(HomeSection.POPULAR)) },
                                        favoriteIds = favoriteIds,
                                        onToggleFavorite = { gameId ->
                                            handleFavoriteToggle(gameId, favoriteIds, screenModel, toastState, strings)
                                        },
                                    )
                                    Spacer(modifier = Modifier.height(24.dp))
                                }
                            }

                            if (recommendedGames.isNotEmpty()) {
                                item(key = "section_recommended") {
                                    GameCompactRow(
                                        title = strings.forYou,
                                        games = recommendedGames,
                                        onGameClick = { navigator.push(GameDetailNavScreen(it.id)) },
                                        onSeeAllClick = {
                                            navigator.push(
                                                SeeAllNavScreen(
                                                    HomeSection.RECOMMENDED,
                                                    preferences.selectedGenres.toList()
                                                )
                                            )
                                        },
                                    )
                                    Spacer(modifier = Modifier.height(24.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun handleFavoriteToggle(
    gameId: Int,
    favoriteIds: Set<Int>,
    screenModel: HomeScreenModel,
    toastState: com.kami.gamelist.core.ui.components.GameToastState,
    strings: com.kami.gamelist.core.ui.localization.AppStrings,
) {
    val wasAdded = gameId !in favoriteIds
    screenModel.toggleFavorite(gameId)
    toastState.show(
        if (wasAdded) strings.addedToFavorites else strings.removedFromFavorites,
        if (wasAdded) GameToastType.SUCCESS else GameToastType.INFO
    )
}
