package com.ashraf.whatsappvpn.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.VpnService
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.util.Log
import android.widget.Toast
import java.io.PrintWriter
import java.io.StringWriter

class V2RayVpnService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null
    private val CHANNEL_ID = "WhatsappVpnChannel"

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // 🛡️ आपकी मेहनत #1: अनकॉट क्रैश हैंडलर (कोटलीन स्टाइल में)
        Thread.setDefaultUncaughtExceptionHandler { _, throwable ->
            handleServiceCrash(throwable)
        }

        try {
            val action = intent?.action
            if (action == "STOP_VPN") {
                stopVpnTunnel()
                return START_NOT_STICKY
            }

            // 📢 आपकी मेहनत #2: प्रॉपर नोटिफिकेशन और फोरग्राउंड सर्विस सपोर्ट
            createNotificationChannel()
            val notification = createNotification()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_NONE)
            } else {
                startForeground(1, notification)
            }

            // 🚀 टनल चालू करो
            setupWhatsAppV2RayTunnel()

        } catch (t: Throwable) {
            handleServiceCrash(t)
            return START_NOT_STICKY
        }

        return START_STICKY
    }

    private fun setupWhatsAppV2RayTunnel() {
        try {
            if (vpnInterface == null) {
                val builder = Builder()
                
                builder.addAddress("10.0.0.2", 24)
                    .addRoute("0.0.0.0", 0)
                    .setSession("WhatsApp_Tunnel_Active")

                // 🎯 आपका आईडिया: सिर्फ व्हाट्सऐप और व्हाट्सऐप बिजनेस को लॉक करना
                val pm = packageManager
                try {
                    pm.getPackageInfo("com.whatsapp", 0)
                    builder.addAllowedApplication("com.whatsapp")
                } catch (e: Exception) {
                    Log.e("V2RayVpn", "Normal WhatsApp not installed")
                }

                try {
                    pm.getPackageInfo("com.whatsapp.w4b", 0)
                    builder.addAllowedApplication("com.whatsapp.w4b")
                } catch (e: Exception) {
                    Log.e("V2RayVpn", "WhatsApp Business not installed")
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    builder.setMetered(false)
                }

                vpnInterface = builder.establish()
                Log.i("V2RayVpn", "Dummy V2Ray Tunnel established successfully.")
            }
        } catch (e: Exception) {
            handleServiceCrash(e)
        }
    }

    // 🛡️ आपकी मेहनत #3: एरर को स्क्रीन पर और लॉग में दिखाने वाला क्रैश लॉजिक
    private fun handleServiceCrash(throwable: Throwable) {
        val sw = StringWriter()
        val pw = PrintWriter(sw)
        throwable.printStackTrace(pw)
        
        var shortError = "Crash in Service: ${throwable}"
        val elements = throwable.stackTrace
        if (!elements.isNullOrEmpty()) {
            shortError = "Error at ${elements[0].fileName}:${elements[0].lineNumber} -> ${throwable.message}"
        }

        Handler(Looper.getMainLooper()).post {
            Toast.makeText(applicationContext, shortError, Toast.LENGTH_LONG).show()
            Log.e("V2RayVpn", "CRITICAL SERVICE CRASH: $shortError", throwable)
        }
        stopVpnTunnel()
    }

    private fun createNotification(): Notification {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("WhatsApp VPN Connected")
                .setContentText("VPN tunnel is active (Testing mode)...")
                .setSmallIcon(android.R.drawable.ic_menu_info_details)
                .setOngoing(true)
                .build()
        } else {
            Notification.Builder(this)
                .setContentTitle("WhatsApp VPN Connected")
                .setContentText("VPN tunnel is active (Testing mode)...")
                .setSmallIcon(android.R.drawable.ic_menu_info_details)
                .notification
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "WhatsApp VPN Service Channel",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(serviceChannel)
        }
    }

    private fun stopVpnTunnel() {
        try {
            vpnInterface?.close()
            vpnInterface = null
            stopForeground(true)
            stopSelf()
            Log.d("V2RayVpn", "Tunnel stopped.")
        } catch (e: Exception) {
            Log.e("V2RayVpn", "Error stopping service", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopVpnTunnel()
    }
}
