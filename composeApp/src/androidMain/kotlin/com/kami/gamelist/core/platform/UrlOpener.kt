package com.kami.gamelist.core.platform

import android.content.Context
import android.content.Intent
import android.net.Uri

actual class UrlOpener(private val context: Context) {
    actual fun open(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
    }
}
