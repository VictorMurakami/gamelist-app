package com.kami.gamelist.core.platform

import platform.Foundation.NSURL
import platform.UIKit.UIApplication

actual class UrlOpener {
    actual fun open(url: String) {
        val nsUrl = NSURL.URLWithString(url) ?: return
        UIApplication.sharedApplication.openURL(nsUrl)
    }
}
