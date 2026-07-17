package com.kami.gamelist.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kami.gamelist.core.ui.modifier.pressScale
import com.kami.gamelist.core.ui.theme.GameTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateListSheet(
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState()
    var name by remember { mutableStateOf("") }
    val colors = GameTheme.colors

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.surfaceBase,
        contentColor = colors.textPrimary,
        scrimColor = colors.backgroundDark.copy(alpha = 0.7f),
        shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
            Text(
                text = "NEW LIST",
                style = GameTheme.typography.headlineSmall,
                color = colors.accent
            )

            Spacer(modifier = Modifier.height(20.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = {
                    Text(
                        "List name",
                        style = GameTheme.typography.labelMedium,
                        color = colors.textMuted
                    )
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(6.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colors.accent,
                    unfocusedBorderColor = colors.borderSubtle,
                    cursorColor = colors.accent,
                    focusedTextColor = colors.textPrimary,
                    unfocusedTextColor = colors.textPrimary,
                    focusedLabelColor = colors.accent,
                    unfocusedLabelColor = colors.textMuted,
                    focusedContainerColor = colors.surfaceElevated,
                    unfocusedContainerColor = colors.surfaceElevated
                ),
                textStyle = GameTheme.typography.bodyLarge
            )

            Spacer(modifier = Modifier.height(24.dp))

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .pressScale(
                        onClick = { if (name.isNotBlank()) onCreate(name) }
                    ),
                shape = RoundedCornerShape(6.dp),
                color = if (name.isNotBlank()) colors.accent.copy(alpha = 0.15f)
                else colors.surfaceOverlay,
                border = BorderStroke(
                    1.dp,
                    if (name.isNotBlank()) colors.accent else colors.borderSubtle
                ),
                tonalElevation = 0.dp
            ) {
                androidx.compose.foundation.layout.Box(
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    Text(
                        text = "CREATE",
                        style = GameTheme.typography.labelLarge,
                        color = if (name.isNotBlank()) colors.accent else colors.textMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
