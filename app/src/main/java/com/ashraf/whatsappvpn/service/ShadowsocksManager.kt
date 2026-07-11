package com.ashraf.whatsappvpn.service

import android.content.Context
import com.github.shadowsocks.Core
import com.github.shadowsocks.database.Profile
import com.github.shadowsocks.database.ProfileManager

object ShadowsocksManager {

    fun startShadowsocksServer(context: Context) {
        try {
            // Shadowsocks के लिए एक डमी प्रोफाइल सेटअप ताकि इंजन शुरू हो सके
            val profile = Profile().apply {
                name = "WhatsApp VPN Secure"
                host = "127.0.0.1" // यहाँ बाद में आपका सर्वर IP आएगा
                remotePort = 8388
                password = "your_password"
                method = "aes-256-gcm"
            }
            
            // प्रोफाइल सेव करके इंजन को कमांड देना
            ProfileManager.clear()
            ProfileManager.createProfile(profile)
            
            // शैडोसॉक्स कोर इंजन को स्टार्ट करना
            Core.startService()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stopShadowsocksServer(context: Context) {
        try {
            // शैडोसॉक्स कोर इंजन को स्टॉप करना
            Core.stopService()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
