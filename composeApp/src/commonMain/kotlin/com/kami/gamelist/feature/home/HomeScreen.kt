package com.kami.gamelist.feature.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.kami.gamelist.core.ui.UiState
import com.kami.gamelist.feature.detail.GameDetailNavScreen
import com.kami.gamelist.core.ui.components.EmptyState
import com.kami.gamelist.core.ui.components.ErrorState
import com.kami.gamelist.core.ui.components.FilterChipRow
import com.kami.gamelist.core.ui.components.GameGrid
import com.kami.gamelist.core.ui.components.GameGridSkeleton
import com.kami.gamelist.core.ui.components.OfflineBanner

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(screenModel: HomeScreenModel) {
    val navigator = LocalNavigator.currentOrThrow
    val uiState: UiState<HomeData> by screenModel.uiState.collectAsState()
    val selectedGenre: String? by screenModel.selectedGenre.collectAsState()
    val selectedPlatform: String? by screenModel.selectedPlatform.collectAsState()
    val isOffline: Boolean by screenModel.isOffline.collectAsState()
    var isRefreshing by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        OfflineBanner(isOffline = isOffline)

        Text(
            text = "Free Games",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
        )

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

            is UiState.Success<HomeData> -> {
                val data = state.data

                if (data.genres.isNotEmpty()) {
                    FilterChipRow(
                        options = data.genres,
                        selectedOption = selectedGenre,
                        onOptionSelected = { screenModel.selectGenre(it) },
                        allLabel = "All Genres"
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                if (data.platforms.isNotEmpty()) {
                    FilterChipRow(
                        options = data.platforms,
                        selectedOption = selectedPlatform,
                        onOptionSelected = { screenModel.selectPlatform(it) },
                        allLabel = "All Platforms"
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (data.games.isEmpty()) {
                    EmptyState(
                        icon = Icons.Outlined.SportsEsports,
                        title = "No games found",
                        subtitle = "Try adjusting your filters"
                    )
                } else {
                    PullToRefreshBox(
                        isRefreshing = isRefreshing,
                        onRefresh = {
                            isRefreshing = true
                            screenModel.refresh()
                            isRefreshing = false
                        },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        GameGrid(
                            games = data.games,
                            onGameClick = { game -> navigator.push(GameDetailNavScreen(game.id)) }
                        )
                    }
                }
            }
        }
    }
}
