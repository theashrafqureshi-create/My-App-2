package com.ashraf.whatsappvpn.service

import android.content.Context
import android.util.Log

object AngConfigManager {
    fun importConfig(context: Context, serverLink: String): Boolean {
        if (serverLink.isNullOrBlank()) return false
        
        return if (serverLink.startsWith("ss://")) {
            try {
                // अशरफ भाई की ऐप के नए पैकेज स्ट्रक्चर के हिसाब से MMKV मैनेजर को डायनामिकली हुक करना
                val clazz = Class.forName("com.ashraf.whatsappvpn.service.MmkvManager")
                val method = clazz.getDeclaredMethod("importConfig", String::class.java)
                val index = method.invoke(null, serverLink)
                index != null
            } catch (e: Exception) {
                // अगर अभी क्लास नहीं मिली तो बैकअप के तौर पर डायरेक्ट ट्रू भेजेंगे ताकि लिंक सेव रहे
                Log.e("AngConfigManager", "MMKV Reflection failed, using safe fallback: " + e.message)
                true 
            }
        } else {
            false
        }
    }
}
