package com.kami.gamelist.core.ui.model

import com.kami.gamelist.data.model.SearchHistory

data class SearchHistoryUi(
    val query: String,
)

fun SearchHistory.toUi() = SearchHistoryUi(query = query)
