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

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "VPN Service Started Manually");
        stopVpn();

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
        builder.setSession("WhatsappVPN")
               .addAddress("10.0.0.2", 24)
               .addRoute("0.0.0.0", 0)
               .addDnsServer("8.8.8.8");

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

    private void stopVpn() {
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
