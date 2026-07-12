package com.ashraf.whatsappvpn.service;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager; // पैकेज चेकिंग के लिए आवश्यक इम्पोर्ट
import android.net.VpnService;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import java.io.IOException;

public class ShadowsocksVpnService extends VpnService {
    private static final String TAG = "ShadowsocksVpn";
    private Thread vpnThread;
    private ParcelFileDescriptor vpnInterface;
    
    // हमारे लोकल सर्वर का कनेक्शन जोड़ना
    private ShadowsocksLocalServer localServer;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "VPN Service Started Manually");
        stopVpn();

        // SharedPreferences से सेव की हुई ss:// लिंक को पढ़ना
        SharedPreferences sharedPref = getSharedPreferences("VpnConfig", Context.MODE_PRIVATE);
        String savedLink = sharedPref.getString("ss_link", "");

        // डिफ़ॉल्ट वैल्यूज (अगर लिंक खाली हो तो ऐप क्रैश नहीं होगी)
        String serverIp = "127.0.0.1";     // यहाँ तुम्हारे असली शैडोसॉक्स सर्वर का IP आएगा
        int serverPort = 8388;              // सर्वर का पोर्ट
        String password = "your_password"; // सर्वर का पासवर्ड
        String method = "AES";              // एन्क्रिप्शन मेथड

        if (!savedLink.isEmpty()) {
            Log.d(TAG, "Saved SS Link Found in Backend: " + savedLink);
            // यहाँ आगे हम ss:// लिंक को बेस-64 से डिकोड करने का लॉजिक जोड़ेंगे
        }

        // लोकल प्रॉक्सी सर्वर को बैकग्राउंड में चालू करना
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
               .addRoute("0.0.0.0", 0);     // पूरा इंटरनेट ट्रैफिक वीपीएन में भेजने के लिए base route

        // --- 🎯 तुम्हारा DNS कोड (सऊदी नेटवर्क पर सर्वर को ब्लॉक होने से बचाने के लिए) ---
        builder.addDnsServer("8.8.8.8"); // Primary (गूगल)
        builder.addDnsServer("1.1.1.1"); // Secondary (क्लाउडफ्लेयर)

        // --- 🎯 तुम्हारा व्हाट्सएप फ़िल्टर कोड (सिर्फ व और व बिजनेस में वीपीएन चलाने के लिए) ---
        
        // 1. पर्सनल व्हाट्सएप को रूट करें
        try {
            builder.addAllowedApplication("com.whatsapp");
            Log.d(TAG, "Strict Routing: Normal WhatsApp added to tunnel");
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Normal WhatsApp is not installed on this device");
        }

        // 2. व्हाट्सएप बिजनेस को रूट करें
        try {
            builder.addAllowedApplication("com.whatsapp.w4b");
            Log.d(TAG, "Strict Routing: WhatsApp Business added to tunnel");
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "WhatsApp Business is not installed on this device");
        }

        // --------------------------------------------------------------------------------

        vpnInterface = builder.establish();
        Log.i(TAG, "VPN Interface Established Successfully with Local Proxy!");

        while (!Thread.currentThread().isInterrupted()) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    private void stopVpn() {
        // लोकल सर्वर को रोकना
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

        // --- चाबी को तुरंत गायब करने के लिए नए कमांड्स ---
        try {
            stopForeground(true); // फ़ोरग्राउंड नोटिफिकेशन हटाएगा
            stopSelf();           // सर्विस को रैम से पूरी तरह बंद करेगा
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
