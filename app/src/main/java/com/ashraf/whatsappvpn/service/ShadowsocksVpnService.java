package com.ashraf.whatsappvpn.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.net.VpnService;
import android.os.Build;
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
            Log.d(TAG, "Stopping VPN Service via Action");
            stopVpn();
            return START_NOT_STICKY;
        }

        Log.d(TAG, "VPN Service Started Manually");

        SharedPreferences sharedPref = getSharedPreferences("VpnConfig", Context.MODE_PRIVATE);
        String savedLink = sharedPref.getString("ss_link", "");

        if (savedLink == null || savedLink.trim().isEmpty()) {
            Toast.makeText(this, "Please copy, paste or scan a valid Shadowsocks link first!", Toast.LENGTH_LONG).show();
            stopSelf();
            return START_NOT_STICKY;
        }

        stopVpn();
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
        
        // 🎯 [एंड्रॉइड 14 क्रैश फिक्स]: डिफ़ॉल्ट नंबर हटाकर सही वीपीएन सर्विस टाइप लॉक किया
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_VPN);
        } else {
            startForeground(1, notification);
        }

        String serverIp = "127.0.0.1";
        int serverPort = 8388;
        String password = "your_password";
        String method = "AES";

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
                                Log.e(TAG, "Base64 decoding failed, using raw userInfo");
                            }
                        }
                    }
                    
                    if (userInfo.contains(":")) {
                        String[] parts = userInfo.split(":", 2);
                        method = parts[0];
                        password = parts[1];
                    }
                }
                
                serverIp = uri.getHost();
                serverPort = uri.getPort();
                if (serverPort == -1) {
                    serverPort = 8388;
                }
                Log.d(TAG, "Successfully Parsed Link -> IP: " + serverIp + ", Port: " + serverPort + ", Method: " + method);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error parsing Shadowsocks URL: " + e.getMessage());
        }

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
        builder.setSession("WhatsAppVPN")
               .addAddress("10.0.0.2", 24)
               .addRoute("0.0.0.0", 0);

        builder.addDnsServer("8.8.8.8");
        builder.addDnsServer("1.1.1.1");

        // 🎯 [व्हाट्सएप स्प्लिट टनलिंग सेफ चेक]: ऐप इंस्टॉल है या नहीं, रन-टाइम पर सुरक्षित जांच
        PackageManager pm = getPackageManager();
        try {
            pm.getPackageInfo("com.whatsapp", 0);
            builder.addAllowedApplication("com.whatsapp");
        } catch (Exception e) {
            Log.e(TAG, "Normal WhatsApp not installed or not available");
        }

        try {
            pm.getPackageInfo("com.whatsapp.w4b", 0);
            builder.addAllowedApplication("com.whatsapp.w4b");
        } catch (Exception e) {
            Log.e(TAG, "WhatsApp Business not installed or not available");
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
            Log.e(TAG, "Error stopping service", e);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopVpn();
    }
}
