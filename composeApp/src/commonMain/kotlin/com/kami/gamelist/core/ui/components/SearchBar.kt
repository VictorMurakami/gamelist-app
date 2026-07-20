package com.kami.gamelist.core.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.kami.gamelist.core.ui.localization.LocalStrings
import com.kami.gamelist.core.ui.model.SearchHistoryUi
import com.kami.gamelist.core.ui.theme.GameTheme

@Composable
fun GameSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    recentSearches: List<SearchHistoryUi>,
    showHistory: Boolean,
    onHistoryItemClick: (String) -> Unit,
    onHistoryItemDelete: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = GameTheme.colors
    val strings = LocalStrings.current
    val focusManager = LocalFocusManager.current

    Column(modifier = modifier) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            placeholder = {
                Text(
                    strings.searchPlaceholder,
                    style = GameTheme.typography.bodyMedium,
                    color = colors.textMuted
                )
            },
            leadingIcon = {
                Icon(
                    Icons.Default.Search,
                    contentDescription = "Search",
                    tint = colors.textMuted
                )
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(
                            Icons.Default.Clear,
                            contentDescription = "Clear search query",
                            tint = colors.textSecondary
                        )
                    }
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(
                onSearch = {
                    onSearch(query)
                    focusManager.clearFocus()
                }
            ),
            shape = RoundedCornerShape(6.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = colors.accent,
                unfocusedBorderColor = colors.borderSubtle,
                cursorColor = colors.accent,
                focusedTextColor = colors.textPrimary,
                unfocusedTextColor = colors.textPrimary,
                focusedContainerColor = colors.surfaceElevated,
                unfocusedContainerColor = colors.surfaceElevated
            ),
            textStyle = GameTheme.typography.bodyMedium
        )

        AnimatedVisibility(visible = showHistory && recentSearches.isNotEmpty()) {
            Column {
                recentSearches.forEach { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onHistoryItemClick(item.query) }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Outlined.History,
                            contentDescription = null,
                            tint = colors.textMuted
                        )
                        Text(
                            text = item.query,
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 12.dp),
                            style = GameTheme.typography.bodyMedium,
                            color = colors.textSecondary
                        )
                        IconButton(onClick = { onHistoryItemDelete(item.query) }) {
                            Icon(
                                Icons.Default.Clear,
                                contentDescription = "Remove ${item.query} from history",
                                tint = colors.textMuted
                            )
                        }
                    }
                }
            }
        }
    }
}
