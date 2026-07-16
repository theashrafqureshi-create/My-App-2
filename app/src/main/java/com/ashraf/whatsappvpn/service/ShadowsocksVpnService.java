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
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.Socket;

public class ShadowsocksVpnService extends VpnService {
    private static final String TAG = "ShadowsocksVpn";
    private static final String CHANNEL_ID = "WhatsappVpnChannel";
    private Thread vpnThread;
    private ParcelFileDescriptor vpnInterface;
    private ShadowsocksLocalServer localServer;

    public boolean protectSocket(Socket socket) {
        return protect(socket);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable throwable) {
                handleServiceCrash(throwable);
            }
        });

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

            final String finalServerIp = intent != null ? intent.getStringExtra("SERVER_IP") : null;
            final int finalServerPort = intent != null ? intent.getIntExtra("SERVER_PORT", 8388) : 8388;
            final String finalPassword = intent != null ? intent.getStringExtra("PASSWORD") : null;
            final String finalMethod = intent != null ? intent.getStringExtra("ENCRYPTION_METHOD") : "aes-256-gcm";
            final String clientName = intent != null ? intent.getStringExtra("CLIENT_NAME") : "";

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

            localServer = new ShadowsocksLocalServer(this);

            vpnThread = new Thread(() -> {
                try {
                    Log.i(TAG, "Starting engine for user: " + clientName);
                    localServer.startServer(finalServerIp, finalServerPort, finalPassword, finalMethod);
                    
                    while (!Thread.currentThread().isInterrupted() && vpnInterface != null) {
                        Thread.sleep(1000);
                    }
                } catch (Throwable t) {
                    handleServiceCrash(t);
                }
            }, "ShadowsocksVpnThread");

            vpnThread.setUncaughtExceptionHandler((thread, throwable) -> handleServiceCrash(throwable));
            vpnThread.start();

        } catch (Throwable t) {
            handleServiceCrash(t);
            return START_NOT_STICKY;
        }

        return START_STICKY;
    }

    private void handleServiceCrash(Throwable throwable) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        String stackTrace = sw.toString();

        String shortError = "Crash in Service: " + throwable.toString();
        StackTraceElement[] elements = throwable.getStackTrace();
        if (elements != null && elements.length > 0) {
            shortError = "Error at " + elements[0].getFileName() + ":" + elements[0].getLineNumber() + " -> " + throwable.getMessage();
        }

        final String finalError = shortError;
        new Handler(Looper.getMainLooper()).post(() -> {
            Toast.makeText(getApplicationContext(), finalError, Toast.LENGTH_LONG).show();
            Log.e(TAG, "CRITICAL SERVICE CRASH: " + finalError, throwable);
        });
        stopVpn();
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            builder.setMetered(false);
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
            try {
                localServer.stopServer();
            } catch (Exception e) {
                Log.e(TAG, "Error stopping local server", e);
            }
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
