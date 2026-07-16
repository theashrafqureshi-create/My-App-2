package com.ashraf.whatsappvpn.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.net.VpnService;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;
import java.io.IOException;
import java.net.URI;

public class ShadowsocksVpnService extends VpnService {
    private static final String TAG = "ShadowsocksVpn";
    private static final String CHANNEL_ID = "WhatsappVpnChannel";
    private Thread vpnThread;
    private ParcelFileDescriptor vpnInterface;
    private ShadowsocksLocalServer localServer;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && "STOP_VPN".equals(intent.getAction())) {
            stopVpn();
            return START_NOT_STICKY;
        }

        createNotificationChannel();
        Notification notification;
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notification = new Notification.Builder(this, CHANNEL_ID)
                    .setContentTitle("WhatsApp VPN Connected")
                    .setContentText("VPN tunnel is active...")
                    .setSmallIcon(android.R.drawable.ic_menu_info_details)
                    .setOngoing(true)
                    .build();
        } else {
            notification = new Notification.Builder(this)
                    .setContentTitle("WhatsApp VPN Connected")
                    .setContentText("VPN tunnel is active...")
                    .setSmallIcon(android.R.drawable.ic_menu_info_details)
                    .getNotification();
        }

        // 🎯 [FIXED] एंड्रॉइड Q (API 29) और उससे ऊपर के लिए प्रॉपर VPN टाइप डिक्लेरेशन यहाँ जुड़ गया है
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_VPN);
        } else {
            startForeground(1, notification);
        }

        final String savedLink = intent != null ? intent.getStringExtra("SERVER_LINK") : null;

        if (savedLink == null || savedLink.trim().isEmpty()) {
            new Handler(Looper.getMainLooper()).post(() -> 
                Toast.makeText(ShadowsocksVpnService.this, "Please copy, paste or scan a valid Shadowsocks link first!", Toast.LENGTH_LONG).show()
            );
            stopVpn();
            return START_NOT_STICKY;
        }

        if (vpnThread != null || vpnInterface != null || localServer != null) {
            stopVpnThreadsOnly();
        }

        try {
            setupVpnInterface();
        } catch (IOException e) {
            Log.e(TAG, "Failed to establish VPN Interface immediately: " + e.getMessage());
            stopVpn();
            return START_NOT_STICKY;
        }

        final String finalServerIp;
        final int finalServerPort;
        final String finalPassword;
        final String finalMethod;

        String tempServerIp = "127.0.0.1";
        int tempServerPort = 8388;
        String tempPassword = "your_password";
        String tempMethod = "AES";

        try {
            if (savedLink.startsWith("ss://")) {
                String cleanLink = savedLink;
                if (cleanLink.contains("#")) {
                    cleanLink = cleanLink.split("#")[0];
                }
                
                URI uri = URI.create(cleanLink);
                String userInfo = uri.getUserInfo();
                
                if (userInfo != null) {
                    if (!userInfo.contains(":")) {
                        try {
                            userInfo = new String(Base64.decode(userInfo, Base64.URL_SAFE | Base64.NO_PADDING | Base64.NO_WRAP));
                        } catch (Exception e) {
                            try {
                                userInfo = new String(Base64.decode(userInfo, Base64.DEFAULT));
                            } catch (Exception ex) {
                                Log.e(TAG, "Base64 decoding failed");
                            }
                        }
                    }
                    
                    if (userInfo.contains(":")) {
                        String[] parts = userInfo.split(":", 2);
                        tempMethod = parts[0];
                        tempPassword = parts[1];
                    }
                }
                
                tempServerIp = uri.getHost();
                tempServerPort = uri.getPort();
                if (tempServerPort == -1) {
                    tempServerPort = 8388;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error parsing Shadowsocks URL: " + e.getMessage());
        }

        finalServerIp = tempServerIp;
        finalServerPort = tempServerPort;
        finalPassword = tempPassword;
        finalMethod = tempMethod;

        localServer = new ShadowsocksLocalServer();

        vpnThread = new Thread(() -> {
            try {
                localServer.startServer(finalServerIp, finalServerPort, finalPassword, finalMethod);
                
                while (!Thread.currentThread().isInterrupted() && vpnInterface != null) {
                    Thread.sleep(1000);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error running VPN thread: " + e.getMessage());
                stopVpn();
            }
        }, "ShadowsocksVpnThread");

        vpnThread.start();
        return START_STICKY;
    }

    private void setupVpnInterface() throws IOException {
        Builder builder = new Builder();
        builder.setSession("WhatsAppVPN")
               .addAddress("10.0.0.2", 24)
               .addRoute("0.0.0.0", 0);

        builder.addDnsServer("8.8.8.8");
        builder.addDnsServer("1.1.1.1");

        PackageManager pm = getPackageManager();
        try {
            pm.getPackageInfo("com.whatsapp", 0);
            builder.addAllowedApplication("com.whatsapp");
        } catch (Exception e) {
            Log.e(TAG, "Normal WhatsApp not installed");
        }

        try {
            pm.getPackageInfo("com.whatsapp.w4b", 0);
            builder.addAllowedApplication("com.whatsapp.w4b");
        } catch (Exception e) {
            Log.e(TAG, "WhatsApp Business not installed");
        }

        vpnInterface = builder.establish();
        Log.i(TAG, "VPN Interface Established Instantly!");
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

    private void stopVpnThreadsOnly() {
        if (localServer != null) {
            localServer.stopServer();
            localServer = null;
        }
        if (vpnInterface != null) {
            try {
                vpnInterface.close();
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

    private void stopVpn() {
        stopVpnThreadsOnly();
        try {
            stopForeground(true);
            stopSelf();
        } catch (Exception e) {
            Log.e(TAG, "Error stopping service", e);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopVpn();
    }
}
