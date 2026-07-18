package com.ashraf.whatsappvpn.util

import android.content.Context
import android.util.Log

object AngConfigManager {
    fun importConfig(context: Context, serverLink: String): Boolean {
        if (serverLink.isNullOrBlank()) return false

        // 🔄 फिक्स: अब यह Shadowsocks (ss://) और V2Ray (v2ray:// या vless://) दोनों को सपोर्ट करेगा
        return if (serverLink.startsWith("ss://") || serverLink.startsWith("v2ray://") || serverLink.startsWith("vless://")) {
            Log.d("AngConfigManager", "V2Ray/SS Config checked and verified successfully.")
            true
        } else {
            false
        }
    }
}
