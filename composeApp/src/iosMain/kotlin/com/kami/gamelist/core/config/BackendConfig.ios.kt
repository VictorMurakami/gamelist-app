package com.kami.gamelist.core.config

actual object BackendConfig {
    // O simulador iOS compartilha a rede do host.
    actual val baseUrl: String = "http://localhost:8000"
}
