package com.kami.gamelist.core.platform

actual class UrlOpener {
    actual fun open(url: String) {
        // No-op: jvm target exists only so tests can run.
    }
}
