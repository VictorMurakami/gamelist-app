package com.kami.gamelist.core.config

actual object BackendConfig {
    // 10.0.2.2 e como o emulador Android enxerga o localhost da maquina host.
    // Em device fisico, trocar pelo IP da maquina na LAN.
    actual val baseUrl: String = "http://10.0.2.2:8000"
}
