package com.kami.gamelist.data.local

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOne
import com.kami.gamelist.data.model.Game
import com.kami.gamelist.data.model.ListType
import com.kami.gamelist.data.model.SearchHistory
import com.kami.gamelist.data.model.UserList
import com.kami.gamelist.db.GameListDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock

class UserLocalDataSource(private val database: GameListDatabase) {

    private val favoriteQueries = database.favoriteQueries
    private val userListQueries = database.userListQueries
    private val userListEntryQueries = database.userListEntryQueries
    private val searchHistoryQueries = database.searchHistoryQueries

    fun observeFavorites(): Flow<List<Game>> =
        favoriteQueries.selectAll()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { entities -> entities.map { it.toDomain() } }

    fun isFavorite(gameId: Int): Flow<Boolean> =
        favoriteQueries.isFavorite(gameId.toLong())
            .asFlow()
            .mapToOne(Dispatchers.IO)
            .map { it > 0 }

    fun toggleFavorite(gameId: Int) {
        val count = favoriteQueries.isFavorite(gameId.toLong()).executeAsOne()
        if (count > 0) {
            favoriteQueries.delete(gameId.toLong())
        } else {
            favoriteQueries.insert(gameId.toLong(), Clock.System.now().toEpochMilliseconds())
        }
    }

    fun observeLists(): Flow<List<UserList>> =
        userListQueries.selectAll()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { entities -> entities.map { it.toDomain() } }

    fun createList(name: String, type: ListType): Long {
        userListQueries.insert(name, type.name, Clock.System.now().toEpochMilliseconds())
        return userListQueries.lastInsertId().executeAsOne()
    }

    fun updateListName(id: Long, name: String) {
        userListQueries.update(name, id)
    }

    fun deleteList(id: Long) {
        database.transaction {
            userListEntryQueries.deleteAllForList(id)
            userListQueries.delete(id)
        }
    }

    fun addToList(listId: Long, gameId: Int) {
        userListEntryQueries.insert(listId, gameId.toLong(), Clock.System.now().toEpochMilliseconds())
    }

    fun removeFromList(listId: Long, gameId: Int) {
        userListEntryQueries.delete(listId, gameId.toLong())
    }

    fun observeGamesInList(listId: Long): Flow<List<Game>> =
        userListEntryQueries.selectByListId(listId)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { entities -> entities.map { it.toDomain() } }

    fun isInList(listId: Long, gameId: Int): Flow<Boolean> =
        userListEntryQueries.isInList(listId, gameId.toLong())
            .asFlow()
            .mapToOne(Dispatchers.IO)
            .map { it > 0 }

    fun listGameCount(listId: Long): Flow<Long> =
        userListEntryQueries.countByListId(listId)
            .asFlow()
            .mapToOne(Dispatchers.IO)

    fun addSearchQuery(query: String, searchedAt: Long = Clock.System.now().toEpochMilliseconds()) {
        searchHistoryQueries.insert(query, searchedAt)
    }

    fun observeRecentSearches(): Flow<List<SearchHistory>> =
        searchHistoryQueries.selectRecent()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { entities ->
                entities.map { SearchHistory(query = it.query, searchedAt = it.searched_at) }
            }

    fun deleteSearchQuery(query: String) {
        searchHistoryQueries.delete(query)
    }

    fun clearSearchHistory() {
        searchHistoryQueries.deleteAll()
    }
}
