package com.kami.gamelist.data.repository

sealed interface SyncState {
    data object Idle : SyncState
    data object Syncing : SyncState
    data class SyncFailed(val message: String) : SyncState
    data object Synced : SyncState
}
