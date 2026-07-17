package com.kami.gamelist.core.ui.model

import com.kami.gamelist.data.model.ListType
import com.kami.gamelist.data.model.UserList

data class ListUi(
    val id: Long,
    val name: String,
    val typeLabel: String,
    val isDeletable: Boolean,
    val gameCount: Int = 0,
    val createdAt: Long = 0L,
)

fun UserList.toUi() = ListUi(
    id = id,
    name = name,
    typeLabel = type.name.lowercase().replace("_", " ").replaceFirstChar { it.uppercase() },
    isDeletable = type == ListType.CUSTOM,
    createdAt = createdAt,
)
