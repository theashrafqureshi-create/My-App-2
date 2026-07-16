package com.ashraf.whatsappvpn.service;

import android.app.AlertDialog;
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
import android.util.Log;
import android.view.WindowManager;
import android.widget.Toast;
import java.io.IOException;
import java.net.Socket;

public class ShadowsocksVpnService extends VpnService {
    private static final String TAG = "ShadowsocksVpn";
    private static final String CHANNEL_ID = "WhatsappVpnChannel";
    private Thread vpnThread;
    private ParcelFileDescriptor vpnInterface;
    private ShadowsocksLocalServer localServer;

    // 🛠️ नया मेथड: ShadowsocksLocalServer इस मेथड को कॉल करके अपने सॉकेट्स को लूप से बचाएगा
    public boolean protectSocket(Socket socket) {
        return protect(socket);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        try {
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

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_NONE);
            } else {
                startForeground(1, notification);
            }

            // 🛠️ बदलाव: अब हम क्रेडेंशियल्स को सीधे Intent से अलग-अलग वैल्यू के रूप में ले रहे हैं
            final String finalServerIp = intent != null ? intent.getStringExtra("SERVER_IP") : null;
            final int finalServerPort = intent != null ? intent.getIntExtra("SERVER_PORT", 8388) : 8388;
            final String finalPassword = intent != null ? intent.getStringExtra("PASSWORD") : null;
            final String finalMethod = intent != null ? intent.getStringExtra("ENCRYPTION_METHOD") : "aes-256-gcm";
            final String clientName = intent != null ? intent.getStringExtra("CLIENT_NAME") : "";

            // वैलिडेशन चेक: अगर मुख्य डेटा गायब है, तो सर्विस बंद हो जाएगी
            if (finalServerIp == null || finalServerIp.trim().isEmpty() || finalPassword == null || finalPassword.trim().isEmpty()) {
                new Handler(Looper.getMainLooper()).post(() -> 
                    Toast.makeText(ShadowsocksVpnService.this, "Invalid VPN Configurations!", Toast.LENGTH_LONG).show()
                );
                stopVpn();
                return START_NOT_STICKY;
            }

            if (vpnThread != null || vpnInterface != null || localServer != null) {
                stopVpnThreadsOnly();
            }

            setupVpnInterface();

            // 🛠️ बदलाव: अब हम लोकल सर्वर के कंस्ट्रक्टर में 'this' (इस सर्विस का रेफरेंस) पास कर रहे हैं
            localServer = new ShadowsocksLocalServer(this);

            vpnThread = new Thread(() -> {
                try {
                    Log.i(TAG, "Starting engine for user: " + clientName);
                    localServer.startServer(finalServerIp, finalServerPort, finalPassword, finalMethod);
                    
                    while (!Thread.currentThread().isInterrupted() && vpnInterface != null) {
                        Thread.sleep(1000);
                    }
                } catch (Exception e) {
                    showDynamicError("Thread Error: " + e.getMessage());
                    stopVpn();
                }
            }, "ShadowsocksVpnThread");

            vpnThread.start();

        } catch (Throwable t) {
            showDynamicError("Service Crash: " + t.getClass().getSimpleName() + " -> " + t.getMessage());
            stopVpn();
            return START_NOT_STICKY;
        }

        return START_STICKY;
    }

    private void showDynamicError(final String errorMessage) {
        new Handler(Looper.getMainLooper()).post(() -> {
            try {
                Toast.makeText(getApplicationContext(), errorMessage, Toast.LENGTH_LONG).show();
                
                AlertDialog dialog = new AlertDialog.Builder(getApplicationContext())
                        .setTitle("VPN System Alert")
                        .setMessage(errorMessage)
                        .setPositiveButton("OK", null)
                        .create();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY);
                } else {
                    dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
                }
                dialog.show();
            } catch (Exception ignored) {}
        });
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
