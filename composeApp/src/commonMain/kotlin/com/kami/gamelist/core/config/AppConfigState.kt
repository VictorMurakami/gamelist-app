package com.kami.gamelist.core.config

enum class UpdateStatus { NONE, RECOMMENDED, FORCED }

data class UpdateInfo(
    val status: UpdateStatus,
    val latestVersion: String?,
    val storeUrl: String?,
    val changelog: String,
)

data class MaintenanceInfo(
    val active: Boolean,
    val message: String?,
)

data class AppConfigState(
    val update: UpdateInfo,
    val maintenance: MaintenanceInfo,
    val flags: Map<String, Boolean>,
    val issuer: String,
    val clientId: String,
) {
    companion object {
        /** Estado neutro: usado quando nao ha rede nem cache. Nao bloqueia nada. */
        val EMPTY = AppConfigState(
            update = UpdateInfo(UpdateStatus.NONE, null, null, ""),
            maintenance = MaintenanceInfo(active = false, message = null),
            flags = emptyMap(),
            issuer = "",
            clientId = "",
        )
    }
}
