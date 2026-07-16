package com.kami.gamelist.feature.detail

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.PlaylistAdd
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.kami.gamelist.core.ui.components.ListSelector
import com.kami.gamelist.core.ui.components.ScreenshotCarousel
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
        val showListSelector by screenModel.showListSelector.collectAsState()

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        FavoriteButton(
                            isFavorite = isFavorite,
                            onToggle = { screenModel.toggleFavorite() },
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        IconButton(onClick = { screenModel.onShowListSelector() }) {
                            Icon(Icons.AutoMirrored.Outlined.PlaylistAdd, contentDescription = "Add to list")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
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
                                    .clip(MaterialTheme.shapes.medium)
                            )
                        }
                    }
                }

                is UiState.Error -> {
                    ErrorState(
                        message = state.message,
                        onRetry = { },
                        modifier = Modifier.padding(padding)
                    )
                }

                is UiState.Success -> {
                    val detail = state.data
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .verticalScroll(rememberScrollState())
                    ) {
                        AsyncImage(
                            model = detail.game.thumbnail,
                            contentDescription = detail.game.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(16f / 9f)
                                .padding(horizontal = 16.dp)
                                .clip(MaterialTheme.shapes.medium)
                        )

                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = detail.game.title,
                                style = MaterialTheme.typography.headlineMedium
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Row {
                                Text(
                                    text = detail.game.genre,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = detail.game.platform,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = detail.status,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = detail.description,
                                style = MaterialTheme.typography.bodyLarge
                            )

                            if (detail.screenshots.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(24.dp))
                                Text(
                                    text = "Screenshots",
                                    style = MaterialTheme.typography.titleLarge
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                ScreenshotCarousel(screenshots = detail.screenshots)
                            }

                            detail.minimumSystemRequirements?.let { reqs ->
                                Spacer(modifier = Modifier.height(24.dp))
                                Text(
                                    text = "System Requirements",
                                    style = MaterialTheme.typography.titleLarge
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                reqs.os?.let { SystemReqRow("OS", it) }
                                reqs.processor?.let { SystemReqRow("Processor", it) }
                                reqs.memory?.let { SystemReqRow("Memory", it) }
                                reqs.graphics?.let { SystemReqRow("Graphics", it) }
                                reqs.storage?.let { SystemReqRow("Storage", it) }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "Publisher: ${detail.game.publisher}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Developer: ${detail.game.developer}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Release: ${detail.game.releaseDate}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(32.dp))
                        }
                    }
                }
            }

            if (showListSelector) {
                ListSelector(
                    lists = lists,
                    listsContainingGame = emptySet(),
                    onListToggle = { screenModel.toggleList(it) },
                    onDismiss = { screenModel.onDismissListSelector() }
                )
            }
        }
    }
}

@Composable
private fun SystemReqRow(label: String, value: String) {
    Row(modifier = Modifier.padding(vertical = 2.dp)) {
        Text(
            text = "$label: ",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
