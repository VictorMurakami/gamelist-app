package com.kami.gamelist.core.network

import kotlinx.coroutines.flow.Flow

expect class ConnectivityMonitor {
    val isOnline: Flow<Boolean>
}
