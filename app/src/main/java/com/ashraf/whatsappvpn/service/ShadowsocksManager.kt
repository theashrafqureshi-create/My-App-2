package com.ashraf.whatsappvpn.service

import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor

class ShadowsocksVpnService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if ("START" == action) {
            establishVpnTunnel()
        } else if ("STOP" == action) {
            destroyVpnTunnel()
        }
        return START_NOT_STICKY
    }

    private fun establishVpnTunnel() {
        try {
            if (vpnInterface == null) {
                val builder = Builder()
                builder.setSession("WhatsApp VPN Tunnel")
                builder.addAddress("10.0.0.2", 24)
                builder.addDnsServer("8.8.8.8")
                builder.addRoute("0.0.0.0", 0)
                vpnInterface = builder.establish()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun destroyVpnTunnel() {
        try {
            // 1. इंटरफ़ेस क्लोज किया
            vpnInterface?.close()
            vpnInterface = null
            
            // 2. फ़ोरग्राउंड नोटिफिकेशन हटाया
            stopForeground(true)
            
            // 3. सर्विस को रैम से पूरी तरह हार्ड-किल किया (ताकि चाबी गायब हो जाए)
            stopSelf()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        destroyVpnTunnel()
        super.onDestroy()
    }
}
