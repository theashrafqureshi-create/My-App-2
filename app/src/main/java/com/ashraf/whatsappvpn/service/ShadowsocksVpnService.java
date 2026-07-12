package com.ashraf.whatsappvpn.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.VpnService;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import androidx.core.app.NotificationCompat; // ध्यान दें: अगर बिल्ड में एरर आए तो android.app.Notification.Builder भी यूज़ कर सकते हैं
import java.io.IOException;

public class ShadowsocksVpnService extends VpnService {
    private static final String TAG = "ShadowsocksVpn";
    private static final String CHANNEL_ID = "WhatsappVpnChannel";
    private Thread vpnThread;
    private ParcelFileDescriptor vpnInterface;
    private ShadowsocksLocalServer localServer;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "VPN Service Started Manually");
        stopVpn();

        // 🎯 Android 14 क्रैश फिक्स: टनल और लोकल सर्वर से पहले नोटिफिकेशन चैनल बनाना और सर्विस को फ़ोरग्राउंड में लाना
        createNotificationChannel();
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("WhatsApp VPN Connected")
                .setContentText("VPN tunnel is active...")
                .setSmallIcon(android.R.drawable.ic_menu_info_details)
                .setOngoing(true)
                .build();
        
        // सर्विस को सिस्टम के सामने लाइव करना (API 34 के लिए सेफ तरीका)
        startForeground(1, notification);

        // SharedPreferences से सेव की हुई ss:// लिंक को पढ़ना
        SharedPreferences sharedPref = getSharedPreferences("VpnConfig", Context.MODE_PRIVATE);
        String savedLink = sharedPref.getString("ss_link", "");

        // डिफ़ॉल्ट वैल्यूज
        String serverIp = "127.0.0.1";
        int serverPort = 8388;
        String password = "your_password";
        String method = "AES";

        if (!savedLink.isEmpty()) {
            Log.d(TAG, "Saved SS Link Found in Backend: " + savedLink);
        }

        // लोकल प्रॉक्सी सर्वर को चालू करना
        localServer = new ShadowsocksLocalServer();
        localServer.startServer(serverIp, serverPort, password, method);

        vpnThread = new Thread(() -> {
            try {
                runVpn();
            } catch (Exception e) {
                Log.e(TAG, "Error running VPN: " + e.getMessage());
            }
        }, "ShadowsocksVpnThread");

        vpnThread.start();
        return START_STICKY;
    }

    private void runVpn() throws IOException {
        Builder builder = new Builder();
        builder.setSession("WithAppVPN")
               .addAddress("10.0.0.2", 24)
               .addRoute("0.0.0.0", 0);

        // 🎯 तुम्हारा DNS कोड
        builder.addDnsServer("8.8.8.8");
        builder.addDnsServer("1.1.1.1");

        // 🎯 तुम्हारा व्हाट्सएप फ़िल्टर कोड
        try {
            builder.addAllowedApplication("com.whatsapp");
            Log.d(TAG, "Strict Routing: Normal WhatsApp added to tunnel");
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Normal WhatsApp is not installed");
        }

        try {
            builder.addAllowedApplication("com.whatsapp.w4b");
            Log.d(TAG, "Strict Routing: WhatsApp Business added to tunnel");
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "WhatsApp Business is not installed");
        }

        vpnInterface = builder.establish();
        Log.i(TAG, "VPN Interface Established Successfully!");

        while (!Thread.currentThread().isInterrupted()) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "WhatsApp VPN Service Channel",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }

    private void stopVpn() {
        if (localServer != null) {
            localServer.stopServer();
            localServer = null;
        }
        if (vpnInterface != null) {
            try {
                vpnInterface.close();
                Log.d(TAG, "VPN Interface Closed");
            } catch (IOException e) {
                Log.e(TAG, "Error closing interface", e);
            }
            vpnInterface = null;
        }
        if (vpnThread != null) {
            vpnThread.interrupt();
            vpnThread = null;
        }
        try {
            stopForeground(true);
            stopSelf();
        } catch (Exception e) {
            Log.e(TAG, "Error stopping service components", e);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopVpn();
        Log.d(TAG, "VPN Service Destroyed");
    }
}
