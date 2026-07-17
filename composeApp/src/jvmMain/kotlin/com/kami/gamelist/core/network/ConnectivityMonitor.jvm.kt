package com.kami.gamelist.core.network

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

actual class ConnectivityMonitor {
    actual val isOnline: Flow<Boolean> = MutableStateFlow(true)
}
