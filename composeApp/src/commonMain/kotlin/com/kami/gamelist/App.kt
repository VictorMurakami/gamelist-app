package com.kami.gamelist

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.kami.gamelist.core.ui.theme.GameListTheme
import com.kami.gamelist.data.model.ListType
import com.kami.gamelist.data.repository.UserRepository
import com.kami.gamelist.feature.navigation.AppNavigator
import kotlinx.coroutines.flow.first
import org.koin.compose.koinInject

@Composable
fun App() {
    val userRepository = koinInject<UserRepository>()

    LaunchedEffect(Unit) {
        val lists = userRepository.observeLists().first()
        if (lists.isEmpty()) {
            userRepository.createList("Playing", ListType.PLAYING)
            userRepository.createList("Want to Play", ListType.WANT_TO_PLAY)
            userRepository.createList("Played", ListType.PLAYED)
        }
    }

    GameListTheme(darkTheme = true) {
        AppNavigator()
    }
}
