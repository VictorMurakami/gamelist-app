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
import com.kami.gamelist.feature.navigation.AppNavigator
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import org.koin.compose.koinInject

@Composable
fun App() {
    val userRepository = koinInject<UserRepository>()
    val gameRepository = koinInject<GameRepository>()
    val cacheManager = koinInject<CacheManager>()
    val toastState = rememberGameToastState()
    val settingsState = remember { AppSettingsState(cacheManager) }
    val userPreferencesState = remember { UserPreferencesState(cacheManager) }
    val scrollToTopState = remember { ScrollToTopState() }
    var showOnboarding by remember { mutableStateOf(false) }
    var splashReady by remember { mutableStateOf(false) }

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

    LaunchedEffect(syncState) {
        if (syncState is SyncState.Synced || syncState is SyncState.SyncFailed) {
            delay(600)
            splashReady = true
        }
    }

    LaunchedEffect(Unit) {
        delay(3000)
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
            Box(modifier = Modifier.fillMaxSize()) {
                AppNavigator()
                GameToastHost(
                    state = toastState,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 96.dp)
                )

                if (showOnboarding) {
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
