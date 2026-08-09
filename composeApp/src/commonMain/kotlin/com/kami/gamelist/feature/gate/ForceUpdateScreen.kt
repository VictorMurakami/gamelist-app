package com.kami.gamelist.feature.gate

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kami.gamelist.core.config.UpdateInfo
import com.kami.gamelist.core.ui.components.GameSurface
import com.kami.gamelist.core.ui.localization.LocalStrings
import com.kami.gamelist.core.ui.theme.GameTheme

/**
 * Blocking screen shown when the installed version is below the backend's
 * minimum supported version. This is a plain composable, not a Voyager
 * [cafe.adriel.voyager.core.screen.Screen] — it is rendered instead of the
 * app's navigator, never pushed onto it, so there is no back affordance that
 * could bypass the block.
 */
@Composable
fun ForceUpdateScreen(
    update: UpdateInfo,
    onUpdateClick: () -> Unit,
) {
    val colors = GameTheme.colors
    val strings = LocalStrings.current

    Box(
        modifier = Modifier.fillMaxSize().background(colors.backgroundDark),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.SystemUpdate,
                contentDescription = null,
                tint = colors.accent,
                modifier = Modifier.size(56.dp),
            )

            Text(
                text = strings.updateRequiredTitle.uppercase(),
                style = GameTheme.typography.headlineSmall,
                color = colors.textPrimary,
                textAlign = TextAlign.Center,
            )

            Text(
                text = strings.updateRequiredMessage,
                style = GameTheme.typography.bodyMedium,
                color = colors.textSecondary,
                textAlign = TextAlign.Center,
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

            // Sem storeUrl nao ha para onde mandar o usuario. Um botao que nao
            // faz nada e pior que nenhum botao.
            if (update.storeUrl != null) {
                Button(
                    onClick = onUpdateClick,
                    shape = RoundedCornerShape(6.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.accent,
                        contentColor = colors.backgroundDark,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = strings.updateNow.uppercase(),
                        style = GameTheme.typography.labelLarge,
                    )
                }
            }
        }
    }
}
