package com.kami.gamelist.feature.lists

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.kami.gamelist.core.ui.components.ConfirmationDialog
import com.kami.gamelist.core.ui.components.CreateListSheet
import com.kami.gamelist.core.ui.components.EmptyState
import com.kami.gamelist.core.ui.components.GameChip
import com.kami.gamelist.core.ui.components.GameSurface
import com.kami.gamelist.core.ui.components.GameToastType
import com.kami.gamelist.core.ui.components.LocalGameToastState
import com.kami.gamelist.core.ui.components.LocalScrollToTop
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import com.kami.gamelist.core.ui.components.SectionHeader
import com.kami.gamelist.core.ui.model.ListUi
import com.kami.gamelist.core.ui.modifier.pressScale
import com.kami.gamelist.core.ui.localization.LocalStrings
import com.kami.gamelist.core.ui.theme.GameTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListsScreen(screenModel: ListsScreenModel) {
    val navigator = LocalNavigator.currentOrThrow
    val lists by screenModel.lists.collectAsState()
    val sortOption by screenModel.sortOption.collectAsState()
    var showCreateSheet by remember { mutableStateOf(false) }
    var listToDelete by remember { mutableStateOf<ListUi?>(null) }
    val toastState = LocalGameToastState.current
    val colors = GameTheme.colors
    val strings = LocalStrings.current
    val listState = rememberLazyListState()
    val scrollToTop = LocalScrollToTop.current

    val sortLabels = mapOf(
        ListSortOption.NEWEST to strings.sortNewest,
        ListSortOption.NAME_ASC to strings.sortAZ,
        ListSortOption.GAME_COUNT to strings.sortMostGames,
    )

    LaunchedEffect(scrollToTop.trigger) {
        if (scrollToTop.trigger > 0) {
            listState.animateScrollToItem(0)
        }
    }

    Scaffold(
        containerColor = colors.backgroundDark,
        contentWindowInsets = WindowInsets(0),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateSheet = true },
                containerColor = colors.accent.copy(alpha = 0.15f),
                contentColor = colors.accent,
                shape = RoundedCornerShape(6.dp),
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create new list")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            SectionHeader(
                title = if (lists.isEmpty()) strings.myLists else strings.myListsCount(lists.size)
            )

            if (lists.isNotEmpty()) {
                Text(
                    text = strings.sort,
                    style = GameTheme.typography.labelSmall,
                    color = colors.textMuted,
                    modifier = Modifier.padding(start = 16.dp, bottom = 4.dp)
                )
                Row(
                    modifier = Modifier
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ListSortOption.entries.forEach { option ->
                        GameChip(
                            label = sortLabels.getValue(option),
                            selected = option == sortOption,
                            onClick = { screenModel.setSortOption(option) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (lists.isEmpty()) {
                EmptyState(
                    icon = Icons.AutoMirrored.Outlined.List,
                    title = strings.noListsYet,
                    subtitle = strings.createListToOrganize
                )
            } else {
                LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                    items(lists, key = { it.id }) { list ->
                        SwipeableListItem(
                            list = list,
                            onClick = { navigator.push(ListDetailNavScreen(list.id, list.name)) },
                            onDelete = { listToDelete = list },
                            modifier = Modifier.animateItem()
                        )
                    }
                }
            }
        }

        if (showCreateSheet) {
            CreateListSheet(
                onDismiss = { showCreateSheet = false },
                onCreate = { name ->
                    screenModel.createList(name)
                    showCreateSheet = false
                    toastState.show(strings.listCreated(name), GameToastType.SUCCESS)
                }
            )
        }

        listToDelete?.let { list ->
            ConfirmationDialog(
                title = strings.deleteListQuestion(list.name),
                message = strings.deleteListMessage(),
                confirmLabel = strings.delete,
                onConfirm = {
                    screenModel.deleteList(list.id)
                    toastState.show(strings.listDeleted(list.name), GameToastType.SUCCESS)
                },
                onDismiss = { listToDelete = null }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableListItem(
    list: ListUi,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = GameTheme.colors
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart && list.isDeletable) {
                onDelete()
            }
            false
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .background(colors.error.copy(alpha = 0.12f), RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.CenterEnd
            ) {
                Row(
                    modifier = Modifier.padding(end = 24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = null,
                        tint = colors.error,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = LocalStrings.current.delete.uppercase(),
                        style = GameTheme.typography.labelSmall,
                        color = colors.error
                    )
                }
            }
        },
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = list.isDeletable,
        modifier = modifier
    ) {
        ListItemContent(
            list = list,
            onClick = onClick,
            onDelete = onDelete
        )
    }
}

@Composable
private fun ListItemContent(
    list: ListUi,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    val colors = GameTheme.colors
    val strings = LocalStrings.current

    GameSurface(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .pressScale(onClick = onClick),
        backgroundColor = colors.surfaceElevated,
        borderColor = colors.borderSubtle,
        cornerRadius = 6.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = list.name,
                    style = GameTheme.typography.titleMedium,
                    color = colors.textPrimary
                )
                Text(
                    text = buildString {
                        append(list.typeLabel)
                        append(" · ")
                        append(strings.gameCountLabel(list.gameCount))
                    },
                    style = GameTheme.typography.labelSmall,
                    color = colors.textMuted
                )
            }

            if (list.isDeletable) {
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete ${list.name}",
                        tint = colors.error,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
