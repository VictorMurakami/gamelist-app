package com.kami.gamelist

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kami.gamelist.core.config.AppConfigRepository
import com.kami.gamelist.core.config.AppConfigState
import com.kami.gamelist.core.config.UpdateStatus
import com.kami.gamelist.core.platform.UrlOpener
import com.kami.gamelist.core.ui.components.AnimatedSplashScreen
import com.kami.gamelist.core.ui.components.AppSettingsState
import com.kami.gamelist.core.ui.components.GameToastHost
import com.kami.gamelist.core.ui.components.LocalAppSettings
import com.kami.gamelist.core.ui.components.LocalGameToastState
import com.kami.gamelist.core.ui.components.LocalScrollToTop
import com.kami.gamelist.core.ui.components.LocalUserPreferences
import com.kami.gamelist.core.ui.components.OnboardingSheet
import com.kami.gamelist.core.ui.components.ScrollToTopState
import com.kami.gamelist.core.ui.components.UserPreferencesState
import com.kami.gamelist.core.ui.components.rememberGameToastState
import com.kami.gamelist.core.ui.localization.AppStrings
import com.kami.gamelist.core.ui.localization.LocalStrings
import com.kami.gamelist.core.ui.model.Language
import com.kami.gamelist.core.ui.theme.GameListTheme
import com.kami.gamelist.data.local.CacheManager
import com.kami.gamelist.data.model.ListType
import com.kami.gamelist.data.repository.GameRepository
import com.kami.gamelist.data.repository.SyncState
import com.kami.gamelist.data.repository.UserRepository
import com.kami.gamelist.feature.gate.ForceUpdateScreen
import com.kami.gamelist.feature.gate.MaintenanceScreen
import com.kami.gamelist.feature.navigation.AppNavigator
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import org.koin.compose.koinInject

// AppConfigRepository.load() se limita internamente (ver FETCH_TIMEOUT_MS
// nessa classe) a um teto menor que este, entao ele sempre resolve — com um
// resultado real ou EMPTY — antes deste teto poder disparar, mesmo contra um
// host que aceita a conexao e nunca responde. Ver o comentario ao lado de
// FETCH_TIMEOUT_MS para o porque desse limite viver no repository e nao aqui.
private const val SPLASH_CEILING_MS = 3_000L

@Composable
fun App() {
    val userRepository = koinInject<UserRepository>()
    val gameRepository = koinInject<GameRepository>()
    val cacheManager = koinInject<CacheManager>()
    val appConfigRepository = koinInject<AppConfigRepository>()
    val urlOpener = koinInject<UrlOpener>()
    val toastState = rememberGameToastState()
    val settingsState = remember { AppSettingsState(cacheManager) }
    val userPreferencesState = remember { UserPreferencesState(cacheManager) }
    val scrollToTopState = remember { ScrollToTopState() }
    var showOnboarding by remember { mutableStateOf(false) }
    var splashReady by remember { mutableStateOf(false) }
    // null enquanto a config ainda nao chegou (nem do backend, nem do cache,
    // nem do EMPTY de fallback) — usado so para segurar o splash mais abaixo.
    var appConfig by remember { mutableStateOf<AppConfigState?>(null) }

    val syncState by gameRepository.syncState.collectAsState()

    val strings = when (settingsState.language) {
        Language.EN -> AppStrings.En
        Language.PT_BR -> AppStrings.PtBr
    }

    LaunchedEffect(Unit) {
        val lists = userRepository.observeLists().first()
        if (lists.isEmpty()) {
            userRepository.createList("Playing", ListType.PLAYING)
            userRepository.createList("Want to Play", ListType.WANT_TO_PLAY)
            userRepository.createList("Played", ListType.PLAYED)
        }

        if (!cacheManager.isOnboardingSeen()) {
            showOnboarding = true
        }

        gameRepository.refreshGames()
    }

    // Independente do LaunchedEffect acima: carrega em paralelo com o
    // refreshGames(), nao depois dele, senao a latencia do backend de config
    // somaria a da FreeToGame no splash.
    LaunchedEffect(Unit) {
        val lang = when (settingsState.language) {
            Language.PT_BR -> "pt"
            Language.EN -> "en"
        }
        appConfig = appConfigRepository.load(lang)
    }

    LaunchedEffect(syncState, appConfig) {
        // O gate so pode decidir com a config resolvida (sucesso, cache ou
        // EMPTY) — liberar o splash antes disso mostraria a Home por um
        // instante antes de jogar o usuario numa tela de bloqueio.
        if ((syncState is SyncState.Synced || syncState is SyncState.SyncFailed) && appConfig != null) {
            delay(600)
            splashReady = true
        }
    }

    LaunchedEffect(Unit) {
        // Teto de tempo: um backend lento (de jogos ou de config) nao pode
        // prender o usuario no splash indefinidamente.
        delay(SPLASH_CEILING_MS)
        splashReady = true
    }

    GameListTheme(
        themeMode = settingsState.themeMode,
        accentOption = settingsState.accentOption,
        gridColumns = settingsState.gridColumns,
    ) {
        CompositionLocalProvider(
            LocalGameToastState provides toastState,
            LocalAppSettings provides settingsState,
            LocalUserPreferences provides userPreferencesState,
            LocalScrollToTop provides scrollToTopState,
            LocalStrings provides strings,
        ) {
            // Ordem importa: manutencao e temporaria, versao obsoleta nao.
            // Um usuario preso numa versao morta durante uma manutencao
            // precisa ver a tela de atualizar, que e a unica das duas que
            // ele pode resolver.
            val config = appConfig
            val isForcedUpdate = config?.update?.status == UpdateStatus.FORCED
            val isMaintenance = !isForcedUpdate && config?.maintenance?.active == true

            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    isForcedUpdate -> ForceUpdateScreen(
                        update = config!!.update,
                        onUpdateClick = { config.update.storeUrl?.let(urlOpener::open) },
                    )
                    isMaintenance -> MaintenanceScreen(config!!.maintenance)
                    else -> AppNavigator()
                }
                GameToastHost(
                    state = toastState,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 96.dp)
                )

                if (showOnboarding && !isForcedUpdate && !isMaintenance) {
                    OnboardingSheet(
                        onDismiss = {
                            showOnboarding = false
                            cacheManager.markOnboardingSeen()
                        }
                    )
                }

                AnimatedVisibility(
                    visible = !splashReady,
                    exit = fadeOut(tween(500)),
                    enter = fadeIn(tween(0)),
                ) {
                    AnimatedSplashScreen()
                }
            }
        }
    }
}
