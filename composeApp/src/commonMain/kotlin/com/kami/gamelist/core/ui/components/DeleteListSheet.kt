package com.kami.gamelist.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kami.gamelist.core.ui.localization.LocalStrings
import com.kami.gamelist.core.ui.modifier.pressScale
import com.kami.gamelist.core.ui.theme.GameTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeleteListSheet(
    listName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
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
        shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Filled.Delete,
                contentDescription = null,
                tint = colors.error,
                modifier = Modifier.size(32.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = strings.deleteList,
                style = GameTheme.typography.headlineSmall,
                color = colors.error
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = strings.deleteConfirmMessage(listName),
                style = GameTheme.typography.bodyMedium,
                color = colors.textSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            Surface(
                modifier = Modifier.fillMaxWidth().pressScale(onClick = onConfirm),
                shape = RoundedCornerShape(6.dp),
                color = colors.error.copy(alpha = 0.15f),
                border = BorderStroke(1.dp, colors.error.copy(alpha = 0.4f))
            ) {
                Text(
                    text = strings.delete.uppercase(),
                    style = GameTheme.typography.labelLarge,
                    color = colors.error,
                    modifier = Modifier.padding(vertical = 14.dp).fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Surface(
                modifier = Modifier.fillMaxWidth().pressScale(onClick = onDismiss),
                shape = RoundedCornerShape(6.dp),
                color = colors.surfaceOverlay,
                border = BorderStroke(1.dp, colors.borderSubtle)
            ) {
                Text(
                    text = strings.cancel.uppercase(),
                    style = GameTheme.typography.labelLarge,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(vertical = 14.dp).fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
