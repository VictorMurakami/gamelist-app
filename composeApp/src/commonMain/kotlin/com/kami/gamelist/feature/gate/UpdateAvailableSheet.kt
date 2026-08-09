package com.kami.gamelist.feature.gate

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kami.gamelist.core.config.UpdateInfo
import com.kami.gamelist.core.ui.components.GameSurface
import com.kami.gamelist.core.ui.localization.LocalStrings
import com.kami.gamelist.core.ui.theme.GameTheme

/**
 * Dismissible counterpart to [ForceUpdateScreen]: shown as an overlay above a
 * working Home rather than in place of it. Callers are responsible for the
 * dismissal memory (see `CacheManager.isUpdateDismissed`/`markUpdateDismissed`)
 * — this composable itself has no opinion on whether it should be shown again.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateAvailableSheet(
    update: UpdateInfo,
    onUpdate: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = GameTheme.colors
    val strings = LocalStrings.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.surfaceElevated,
        contentColor = colors.textPrimary,
        scrimColor = colors.backgroundDark.copy(alpha = 0.7f),
        shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = strings.updateAvailableTitle,
                style = GameTheme.typography.headlineSmall,
                color = colors.accent,
            )

            Text(
                text = strings.updateAvailableMessage,
                style = GameTheme.typography.bodyMedium,
                color = colors.textSecondary,
            )

            if (update.changelog.isNotBlank()) {
                GameSurface(cornerRadius = 6.dp) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = strings.whatsNew.uppercase(),
                            style = GameTheme.typography.labelSmall,
                            color = colors.textMuted,
                        )
                        Text(
                            text = update.changelog,
                            style = GameTheme.typography.bodySmall,
                            color = colors.textSecondary,
                        )
                    }
                }
            }

            // Sem storeUrl nao ha para onde mandar o usuario — mesmo caso do
            // ForceUpdateScreen, que trata storeUrl nulo/em branco como ausente.
            if (!update.storeUrl.isNullOrBlank()) {
                Button(
                    onClick = onUpdate,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(6.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.accent,
                        contentColor = colors.backgroundDark,
                    ),
                ) {
                    Text(
                        text = strings.updateNow,
                        style = GameTheme.typography.labelLarge,
                        modifier = Modifier.padding(vertical = 4.dp),
                    )
                }
            }

            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(6.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = colors.textSecondary,
                ),
                border = BorderStroke(1.dp, colors.borderSubtle),
            ) {
                Text(
                    text = strings.later,
                    style = GameTheme.typography.labelLarge,
                    modifier = Modifier.padding(vertical = 4.dp),
                )
            }
        }
    }
}
