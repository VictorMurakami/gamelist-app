package com.kami.gamelist.feature.gate

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kami.gamelist.core.config.MaintenanceInfo
import com.kami.gamelist.core.ui.localization.LocalStrings
import com.kami.gamelist.core.ui.theme.GameTheme

/**
 * Blocking screen shown during a maintenance window. Plain composable, not a
 * Voyager Screen — rendered instead of the app's navigator so there is no
 * back affordance and no action to take.
 */
@Composable
fun MaintenanceScreen(maintenance: MaintenanceInfo) {
    val colors = GameTheme.colors
    val strings = LocalStrings.current

    // O backend passou a exigir mensagem quando a manutencao e ativada, mas uma
    // resposta cacheada de antes dessa validacao pode vir vazia ou em branco —
    // e uma tela de bloqueio em branco e pior que uma generica.
    val message = maintenance.message
        ?.takeIf { it.isNotBlank() }
        ?: strings.maintenanceDefaultMessage

    // See ForceUpdateScreen for why fillMaxSize() must precede verticalScroll():
    // it preserves a min-height == viewport constraint that lets Alignment.Center
    // still center short content, while the relaxed max lets a long message grow
    // and scroll instead of being clipped.
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.backgroundDark)
            .verticalScroll(rememberScrollState()),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.Build,
                contentDescription = null,
                tint = colors.textMuted,
                modifier = Modifier.size(56.dp),
            )

            Text(
                text = strings.maintenanceTitle.uppercase(),
                style = GameTheme.typography.headlineSmall,
                color = colors.textPrimary,
                textAlign = TextAlign.Center,
            )

            Text(
                text = message,
                style = GameTheme.typography.bodyMedium,
                color = colors.textSecondary,
                textAlign = TextAlign.Center,
            )
        }
    }
}
