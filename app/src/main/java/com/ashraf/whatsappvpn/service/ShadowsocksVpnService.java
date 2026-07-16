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
import android.util.Base64;
import android.util.Log;
import android.view.WindowManager;
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

            setupVpnInterface();

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
                    
                    String rawData = cleanLink.substring(5);
                    
                    // 🎯 [SMART DECODER] - अगर पूरा डेटा ही Base64 पैक्ड है (बिना @ और : के)
                    if (!rawData.contains("@") && !rawData.contains(":")) {
                        String decodedFull = "";
                        try {
                            decodedFull = new String(Base64.decode(rawData, Base64.URL_SAFE | Base64.NO_PADDING | Base64.NO_WRAP));
                        } catch (Exception e) {
                            try {
                                decodedFull = new String(Base64.decode(rawData, Base64.DEFAULT));
                            } catch (Exception ex) {
                                Log.e(TAG, "Full Base64 decoding failed");
                            }
                        }
                        
                        // अगर डिकोड होने के बाद इसमें @ मिला, तो इसे मैन्युअली स्प्लिट करके डेटा निकाल लेंगे
                        if (!decodedFull.isEmpty() && decodedFull.contains("@") && decodedFull.contains(":")) {
                            String[] atParts = decodedFull.split("@", 2);
                            String creds = atParts[0];
                            String endpoint = atParts[1];

                            if (creds.contains(":")) {
                                String[] credParts = creds.split(":", 2);
                                tempMethod = credParts[0];
                                tempPassword = credParts[1];
                            }

                            if (endpoint.contains(":")) {
                                int colonIndex = endpoint.lastIndexOf(":");
                                tempServerIp = endpoint.substring(0, colonIndex);
                                try {
                                    tempServerPort = Integer.parseInt(endpoint.substring(colonIndex + 1));
                                } catch (NumberFormatException e) {
                                    tempServerPort = 8388;
                                }
                            } else {
                                tempServerIp = endpoint;
                            }
                            
                            // इस केस के लिए cleanLink को ऐसा सेट कर देते हैं जिससे नीचे का पुराना पार्सर एरर न दे
                            cleanLink = "ss://your_password@" + tempServerIp + ":" + tempServerPort;
                        }
                    }

                    // 🎯 आपका पुराना ओरिजिनल URI पार्सर ब्लॉक (100% वैसा ही है)
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
                                    Log.e(TAG, "UserInfo Base64 decoding failed");
                                }
                            }
                        }
                        
                        if (userInfo.contains(":")) {
                            String[] parts = userInfo.split(":", 2);
                            // अगर ऊपर स्मार्ट डिकोडर से वैल्यू मिल चुकी है, तो उसे ओवरराइट नहीं करेंगे
                            if (tempPassword.equals("your_password")) {
                                tempMethod = parts[0];
                                tempPassword = parts[1];
                            }
                        }
                    }
                    
                    // अगर ऊपर स्मार्ट डिकोडर से IP मिल चुका है, तो उसे ही रखेंगे, वरना पुराना तरीका लेंगे
                    if (tempServerIp.equals("127.0.0.1") && uri.getHost() != null) {
                        tempServerIp = uri.getHost();
                    }
                    if (tempServerPort == 8388 && uri.getPort() != -1) {
                        tempServerPort = uri.getPort();
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
