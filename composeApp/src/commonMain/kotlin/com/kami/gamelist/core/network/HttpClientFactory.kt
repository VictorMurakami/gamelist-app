package com.kami.gamelist.core.network

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.URLProtocol
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

object HttpClientFactory {

    private const val BASE_HOST = "www.freetogame.com"

    fun create(): HttpClient = HttpClient {
        defaultRequest {
            url {
                protocol = URLProtocol.HTTPS
                host = BASE_HOST
            }
        }

        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }

        install(HttpTimeout) {
            requestTimeoutMillis = 15_000
            connectTimeoutMillis = 10_000
        }

        install(HttpRequestRetry) {
            retryOnServerErrors(maxRetries = 2)
            exponentialDelay()
        }

        install(Logging) {
            level = LogLevel.HEADERS
        }
    }

    fun createBackend(): HttpClient = HttpClient {
        // 400 e 429 viram excecao em vez de tentarem desserializar um corpo
        // de erro como AppConfigDto. O repository trata ambos como falha.
        expectSuccess = true

        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }

        install(HttpTimeout) {
            // Quem realmente limita a espera do splash e o
            // withTimeoutOrNull(FETCH_TIMEOUT_MS = 2_500) em
            // AppConfigRepository.load(), que sempre dispara primeiro. Este
            // timeout do Ktor e so um backstop abaixo dele, para o caso (fora
            // do controle do repository) de a conexao nem ser aceita/recusada
            // a tempo por outros motivos de rede.
            requestTimeoutMillis = 3_000
            connectTimeoutMillis = 3_000
        }

        install(Logging) {
            level = LogLevel.HEADERS
        }
    }
}
