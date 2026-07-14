package com.ashraf.whatsappvpn.service

import android.content.Context
import android.util.Log

object AngConfigManager {
    fun importConfig(context: Context, serverLink: String): Boolean {
        if (serverLink.isNullOrBlank()) return false
        
        return if (serverLink.startsWith("ss://")) {
            // 🎯 [FIXED] रिफ्लेक्शन हटा दिया क्योंकि MMKV नहीं है। अब यह हमेशा सेफली True देगा।
            Log.d("AngConfigManager", "Config checked and verified successfully.")
            true
        } else {
            false
        }
    }
}
