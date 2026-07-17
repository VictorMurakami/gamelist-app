package com.kami.gamelist.data.model

data class UserList(
    val id: Long,
    val name: String,
    val type: ListType,
    val createdAt: Long
)

enum class ListType {
    PLAYING, WANT_TO_PLAY, PLAYED, CUSTOM
}

data class UserListEntry(
    val listId: Long,
    val gameId: Int,
    val addedAt: Long
)

data class SearchHistory(
    val query: String,
    val searchedAt: Long
)
