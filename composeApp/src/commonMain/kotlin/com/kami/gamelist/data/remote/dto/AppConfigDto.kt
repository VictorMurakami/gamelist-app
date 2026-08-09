package com.kami.gamelist.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AppConfigDto(
    val update: UpdateDto,
    val maintenance: MaintenanceDto,
    val flags: Map<String, Boolean> = emptyMap(),
    val auth: AuthDto,
)

@Serializable
data class UpdateDto(
    val status: String,
    @SerialName("latest_version") val latestVersion: String? = null,
    @SerialName("store_url") val storeUrl: String? = null,
    val changelog: String = "",
)

@Serializable
data class MaintenanceDto(
    val active: Boolean = false,
    val message: String? = null,
)

@Serializable
data class AuthDto(
    val issuer: String = "",
    @SerialName("client_id") val clientId: String = "",
)
