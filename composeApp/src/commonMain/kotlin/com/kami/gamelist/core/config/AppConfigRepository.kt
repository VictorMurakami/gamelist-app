package com.kami.gamelist.core.config

import com.kami.gamelist.data.local.TextPrefDataSource
import com.kami.gamelist.data.remote.dto.AppConfigDto
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class AppConfigRepository(
    private val api: AppConfigApi,
    private val textPrefs: TextPrefDataSource,
    private val deviceIdProvider: DeviceIdProvider,
    private val appInfo: AppInfo,
) {

    companion object {
        private const val CACHE_KEY = "app_config_cache"

        // Chamador (App.kt) tem um teto de splash de 3s que libera a UI
        // independente do que estiver acontecendo aqui. Esse valor precisa
        // ficar abaixo daquele teto, com folga — ver o comentario ao lado do
        // teto do splash em App.kt. A garantia nao depende de os dois nunca
        // desalinharem: withTimeoutOrNull cancela so o fetch de rede, nao a
        // funcao inteira, entao o fallback pro cache (leitura local, rapida)
        // sempre roda depois, e load() sempre retorna dentro de
        // ~FETCH_TIMEOUT_MS + uma leitura de disco, nunca preso nos 3s de
        // timeout do HttpClientFactory.createBackend() num host que aceita a
        // conexao e nunca responde.
        private const val FETCH_TIMEOUT_MS = 2_500L
    }

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    /**
     * Busca a config no backend, cacheia, e cai para o cache em qualquer falha.
     *
     * Nunca lanca. Sem rede e sem cache devolve AppConfigState.EMPTY, que
     * deixa o app abrir normalmente — a alternativa seria travar o usuario
     * por causa de um problema do servidor.
     */
    suspend fun load(lang: String): AppConfigState {
        // O timeout envolve so o fetch, nao a funcao load() inteira: se
        // envolvesse tudo, cancelar por timeout tambem cancelaria o fallback
        // pro cache logo abaixo, e um backend que trava (aceita a conexao e
        // nunca responde) por mais que FETCH_TIMEOUT_MS faria o usuario
        // perder um forced-update ou maintenance validos que estavam
        // cacheados, em vez de apenas ficar sem a atualizacao mais recente.
        val fetched = withTimeoutOrNull(FETCH_TIMEOUT_MS) {
            runCatching {
                api.fetch(
                    platform = appInfo.platform,
                    version = appInfo.version,
                    deviceId = deviceIdProvider.deviceId(),
                    lang = lang,
                )
            }.getOrNull()
        }

        if (fetched != null) {
            runCatching {
                textPrefs.set(CACHE_KEY, json.encodeToString(CachedConfig(appInfo.version, fetched)))
            }
            return fetched.toState()
        }

        return cachedState() ?: AppConfigState.EMPTY
    }

    /**
     * Envelope do cache: a resposta guardada mais a versao do app que a
     * produziu.
     *
     * A versao nao e metadado decorativo — a resposta do backend e uma funcao
     * da versao que perguntou. Uma resposta `forced` obtida em 1.0.0 nao diz
     * nada sobre 2.0.0; reaproveita-la depois de o usuario atualizar tranca
     * um app que ja esta em dia numa tela sem saida, cujo unico botao leva a
     * uma loja onde nao ha o que baixar. Ver
     * `discardsCacheFromAnotherAppVersion` em AppConfigRepositoryTest.
     */
    @Serializable
    private data class CachedConfig(
        @SerialName("app_version") val appVersion: String,
        val config: AppConfigDto,
    )

    private fun cachedState(): AppConfigState? {
        val raw = textPrefs.get(CACHE_KEY) ?: return null
        // Cache de formato antigo (sem envelope) ou corrompido cai aqui e vira
        // null, que o chamador trata como "sem cache" — o mesmo caminho de uma
        // instalacao nova, nunca uma excecao.
        val cached = runCatching { json.decodeFromString<CachedConfig>(raw) }.getOrNull() ?: return null
        if (cached.appVersion != appInfo.version) return null
        return cached.config.toState()
    }
}

private fun AppConfigDto.toState() = AppConfigState(
    update = UpdateInfo(
        status = update.status.toUpdateStatus(),
        latestVersion = update.latestVersion,
        storeUrl = update.storeUrl,
        changelog = update.changelog,
    ),
    maintenance = MaintenanceInfo(
        active = maintenance.active,
        message = maintenance.message,
    ),
    flags = flags,
    issuer = auth.issuer,
    clientId = auth.clientId,
)

private fun String.toUpdateStatus(): UpdateStatus = when (this) {
    "forced" -> UpdateStatus.FORCED
    "recommended" -> UpdateStatus.RECOMMENDED
    else -> UpdateStatus.NONE
}
