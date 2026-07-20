package com.kami.gamelist.core.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kami.gamelist.core.ui.localization.LocalStrings
import com.kami.gamelist.core.ui.model.ListUi
import com.kami.gamelist.core.ui.theme.GameTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListSelector(
    lists: List<ListUi>,
    listsContainingGame: Set<Long>,
    onListToggle: (ListUi) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState()
    val colors = GameTheme.colors
    val strings = LocalStrings.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.surfaceBase,
        contentColor = colors.textPrimary,
        scrimColor = colors.backgroundDark.copy(alpha = 0.7f),
        shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = strings.addToList,
                style = GameTheme.typography.headlineSmall,
                color = colors.accent
            )

            Spacer(modifier = Modifier.height(16.dp))

            lists.forEach { list ->
                val isInList = listsContainingGame.contains(list.id)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onListToggle(list) }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = list.name,
                        style = GameTheme.typography.bodyLarge,
                        color = if (isInList) colors.accent else colors.textPrimary,
                        modifier = Modifier.weight(1f)
                    )

                    if (isInList) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "In list",
                            tint = colors.accent
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
