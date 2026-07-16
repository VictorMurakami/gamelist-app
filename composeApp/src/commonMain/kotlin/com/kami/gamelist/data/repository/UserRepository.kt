package com.kami.gamelist.data.repository

import com.kami.gamelist.data.local.UserLocalDataSource
import com.kami.gamelist.data.model.Game
import com.kami.gamelist.data.model.ListType
import com.kami.gamelist.data.model.SearchHistory
import com.kami.gamelist.data.model.UserList
import kotlinx.coroutines.flow.Flow

class UserRepository(private val localDataSource: UserLocalDataSource) {

    fun observeFavorites(): Flow<List<Game>> =
        localDataSource.observeFavorites()

    fun isFavorite(gameId: Int): Flow<Boolean> =
        localDataSource.isFavorite(gameId)

    fun toggleFavorite(gameId: Int) {
        localDataSource.toggleFavorite(gameId)
    }

    fun observeLists(): Flow<List<UserList>> =
        localDataSource.observeLists()

    fun createList(name: String, type: ListType): Long =
        localDataSource.createList(name, type)

    fun updateListName(id: Long, name: String) {
        localDataSource.updateListName(id, name)
    }

    fun deleteList(id: Long) {
        localDataSource.deleteList(id)
    }

    fun addToList(listId: Long, gameId: Int) {
        localDataSource.addToList(listId, gameId)
    }

    fun removeFromList(listId: Long, gameId: Int) {
        localDataSource.removeFromList(listId, gameId)
    }

    fun observeGamesInList(listId: Long): Flow<List<Game>> =
        localDataSource.observeGamesInList(listId)

    fun isInList(listId: Long, gameId: Int): Flow<Boolean> =
        localDataSource.isInList(listId, gameId)

    fun listGameCount(listId: Long): Flow<Long> =
        localDataSource.listGameCount(listId)

    fun addSearchQuery(query: String) {
        localDataSource.addSearchQuery(query)
    }

    fun observeRecentSearches(): Flow<List<SearchHistory>> =
        localDataSource.observeRecentSearches()

    fun deleteSearchQuery(query: String) {
        localDataSource.deleteSearchQuery(query)
    }

    fun clearSearchHistory() {
        localDataSource.clearSearchHistory()
    }
}
