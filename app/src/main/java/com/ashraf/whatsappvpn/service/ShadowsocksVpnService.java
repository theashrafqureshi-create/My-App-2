package com.ashraf.whatsappvpn.service;

import android.content.Intent;
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

        // सर्वर की डिटेल्स जो हमें कनेक्ट करनी हैं (यह आगे तुम्हारी मेन स्क्रीन से आएंगी)
        String serverIp = "127.0.0.1"; // यहाँ तुम्हारे असली शैडोसॉक्स सर्वर का IP आएगा
        int serverPort = 8388;          // सर्वर का पोर्ट
        String password = "your_password"; // सर्वर का पासवर्ड
        String method = "AES";          // एन्क्रिप्शन मेथड

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
               .addRoute("0.0.0.0", 0)     // पूरा इंटरनेट ट्रैफिक वीपीएन में भेजने के लिए
               .addDnsServer("8.8.8.8");

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
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopVpn();
        Log.d(TAG, "VPN Service Destroyed");
    }
}
