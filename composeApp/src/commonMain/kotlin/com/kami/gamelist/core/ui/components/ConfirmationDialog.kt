package com.kami.gamelist.core.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.kami.gamelist.core.ui.theme.GameTheme

@Composable
fun ConfirmationDialog(
    title: String,
    message: String,
    confirmLabel: String = "Confirm",
    dismissLabel: String = "Cancel",
    isDestructive: Boolean = true,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = GameTheme.colors

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                style = GameTheme.typography.titleMedium,
                color = colors.textPrimary
            )
        },
        text = {
            Text(
                text = message,
                style = GameTheme.typography.bodyMedium,
                color = colors.textSecondary
            )
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm()
                onDismiss()
            }) {
                Text(
                    text = confirmLabel,
                    color = if (isDestructive) colors.error else colors.accent
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = dismissLabel,
                    color = colors.textSecondary
                )
            }
        },
        containerColor = colors.surfaceElevated,
        titleContentColor = colors.textPrimary,
        textContentColor = colors.textSecondary,
    )
}
