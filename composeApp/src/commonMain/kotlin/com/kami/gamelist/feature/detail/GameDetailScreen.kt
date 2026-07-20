package com.kami.gamelist.feature.detail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.PlaylistAdd
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import coil3.compose.AsyncImage
import com.kami.gamelist.core.ui.UiState
import com.kami.gamelist.core.ui.components.ErrorState
import com.kami.gamelist.core.ui.components.FavoriteButton
import com.kami.gamelist.core.ui.components.GameToastType
import com.kami.gamelist.core.ui.components.ListSelector
import com.kami.gamelist.core.ui.components.LocalGameToastState
import com.kami.gamelist.core.ui.components.ScreenshotCarousel
import com.kami.gamelist.core.ui.components.SectionHeader
import com.kami.gamelist.core.ui.components.shimmerEffect
import com.kami.gamelist.core.ui.model.GameDetailUi
import com.kami.gamelist.core.ui.localization.LocalStrings
import com.kami.gamelist.core.ui.theme.GameTheme
import org.koin.core.parameter.parametersOf

data class GameDetailNavScreen(val gameId: Int) : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = koinScreenModel<GameDetailScreenModel> { parametersOf(gameId) }
        val uiState by screenModel.uiState.collectAsState()
        val isFavorite by screenModel.isFavorite.collectAsState()
        val lists by screenModel.lists.collectAsState()
        val listsContainingGame by screenModel.listsContainingGame.collectAsState()
        val showListSelector by screenModel.showListSelector.collectAsState()
        val toastState = LocalGameToastState.current
        val colors = GameTheme.colors
        val strings = LocalStrings.current

        Scaffold(
            containerColor = colors.backgroundDark,
            contentWindowInsets = WindowInsets(0),
            topBar = {
                TopAppBar(
                    title = { },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Navigate back",
                                tint = colors.textPrimary
                            )
                        }
                    },
                    actions = {
                        FavoriteButton(
                            isFavorite = isFavorite,
                            onToggle = {
                                val wasAdded = !isFavorite
                                screenModel.toggleFavorite()
                                toastState.show(
                                    if (wasAdded) strings.addedToFavorites else strings.removedFromFavorites,
                                    if (wasAdded) GameToastType.SUCCESS else GameToastType.INFO
                                )
                            },
                            modifier = Modifier.padding(end = 4.dp)
                        )
                        IconButton(onClick = { screenModel.onShowListSelector() }) {
                            Icon(
                                Icons.AutoMirrored.Outlined.PlaylistAdd,
                                contentDescription = "Add game to a list",
                                tint = colors.textSecondary
                            )
                        }
                    },
                    windowInsets = WindowInsets(0),
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = colors.backgroundDark
                    )
                )
            }
        ) { padding ->
            when (val state = uiState) {
                is UiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(padding),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Box(
                                Modifier.fillMaxWidth().aspectRatio(16f / 9f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .shimmerEffect()
                            )
                        }
                    }
                }

                is UiState.Error -> {
                    ErrorState(
                        message = state.message,
                        onRetry = { screenModel.retry() },
                        modifier = Modifier.padding(padding)
                    )
                }

                is UiState.Success -> {
                    GameDetailContent(detail = state.data, modifier = Modifier.padding(padding))
                }
            }

            if (showListSelector) {
                ListSelector(
                    lists = lists,
                    listsContainingGame = listsContainingGame,
                    onListToggle = { screenModel.toggleList(it.id) },
                    onDismiss = { screenModel.onDismissListSelector() }
                )
            }
        }
    }
}

@Composable
private fun GameDetailContent(detail: GameDetailUi, modifier: Modifier = Modifier) {
    val colors = GameTheme.colors
    val strings = LocalStrings.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(6.dp))
        ) {
            AsyncImage(
                model = detail.thumbnail,
                contentDescription = detail.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, colors.backgroundDark.copy(alpha = 0.8f))
                        )
                    )
            )
        }

        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = detail.title,
                style = GameTheme.typography.headlineMedium,
                color = colors.textPrimary
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                InfoChip(text = detail.genre, color = colors.neonCyan)
                InfoChip(text = detail.platform, color = colors.neonPurple)
                InfoChip(text = detail.status, color = colors.neonGreen)
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = detail.description,
                style = GameTheme.typography.bodyLarge,
                color = colors.textSecondary
            )

            if (detail.screenshots.isNotEmpty()) {
                Spacer(modifier = Modifier.height(24.dp))
                SectionHeader(title = strings.screenshots, horizontalPadding = 0.dp)
                Spacer(modifier = Modifier.height(8.dp))
                ScreenshotCarousel(screenshots = detail.screenshots)
            }

            detail.systemRequirements?.let { reqs ->
                Spacer(modifier = Modifier.height(24.dp))
                SectionHeader(title = strings.systemRequirements, horizontalPadding = 0.dp)
                Spacer(modifier = Modifier.height(8.dp))

                reqs.os?.let { SystemReqRow(strings.os, it) }
                reqs.processor?.let { SystemReqRow(strings.processor, it) }
                reqs.memory?.let { SystemReqRow(strings.memory, it) }
                reqs.graphics?.let { SystemReqRow(strings.graphics, it) }
                reqs.storage?.let { SystemReqRow(strings.storage, it) }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row {
                DetailLabel(strings.publisher)
                Spacer(modifier = Modifier.width(4.dp))
                Text(detail.publisher, style = GameTheme.typography.bodyMedium, color = colors.textSecondary)
            }
            Row {
                DetailLabel(strings.developer)
                Spacer(modifier = Modifier.width(4.dp))
                Text(detail.developer, style = GameTheme.typography.bodyMedium, color = colors.textSecondary)
            }
            Row {
                DetailLabel(strings.release)
                Spacer(modifier = Modifier.width(4.dp))
                Text(detail.releaseDate, style = GameTheme.typography.bodyMedium, color = colors.textSecondary)
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun InfoChip(text: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = color.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Text(
            text = text.uppercase(),
            style = GameTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun DetailLabel(text: String) {
    Text(
        text = "$text:",
        style = GameTheme.typography.bodyMedium,
        color = GameTheme.colors.textMuted
    )
}

@Composable
private fun SystemReqRow(label: String, value: String) {
    val colors = GameTheme.colors

    Row(modifier = Modifier.padding(vertical = 2.dp)) {
        Text(
            text = "$label: ",
            style = GameTheme.typography.bodyMedium,
            color = colors.textMuted
        )
        Text(
            text = value,
            style = GameTheme.typography.bodyMedium,
            color = colors.textSecondary
        )
    }
}
