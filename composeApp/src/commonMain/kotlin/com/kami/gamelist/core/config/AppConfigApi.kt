package com.kami.gamelist.core.config

import com.kami.gamelist.data.remote.dto.AppConfigDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

class AppConfigApi(private val client: HttpClient) {

    suspend fun fetch(
        platform: String,
        version: String,
        deviceId: String,
        lang: String,
    ): AppConfigDto = client.get("${BackendConfig.baseUrl}/api/v1/app-config/") {
        parameter("platform", platform)
        parameter("version", version)
        parameter("device_id", deviceId)
        parameter("lang", lang)
    }.body()
}
