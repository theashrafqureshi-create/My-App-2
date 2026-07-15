package com.ashraf.whatsappvpn.ui;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.VpnService;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

// 🎯 सही R क्लास इम्पोर्ट
import com.ashraf.whatsappvpn.R;
import com.ashraf.whatsappvpn.service.ShadowsocksVpnService;

public class MainActivity extends AppCompatActivity {

    private boolean isConnected = false;

    private final ActivityResultLauncher<Intent> vpnPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                Intent intent = VpnService.prepare(MainActivity.this);
                if (intent == null) {
                    new Handler(Looper.getMainLooper()).postDelayed(this::handleVpnConnectionSuccess, 200);
                } else {
                    Toast.makeText(MainActivity.this, "VPN Permission Required to Connect!", Toast.LENGTH_SHORT).show();
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 🛠️ [CRITICAL FIX] पूरा एरर बिना कटे बड़े डायलॉग बॉक्स में देखने का सही कोड
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable throwable) {
                Log.e("APP_CRASH", "Crash detected: ", throwable);
                
                final String convertThrowableToString = android.util.Log.getStackTraceString(throwable);

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Looper.prepare();
                        
                        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(MainActivity.this);
                        builder.setTitle("💥 APP CRASHED!");
                        builder.setMessage(convertThrowableToString); 
                        builder.setCancelable(false);
                        builder.setPositiveButton("OK", new android.content.DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(android.content.DialogInterface dialog, int which) {
                                android.os.Process.killProcess(android.os.Process.myPid());
                                System.exit(10);
                            }
                        });
                        
                        builder.show();
                        Looper.loop();
                    }
                }).start();

                try { Thread.sleep(30000); } catch (InterruptedException e) {}
            }
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }

        Button btnConnect = findViewById(R.id.btnConnect);
        RelativeLayout greetingBanner = findViewById(R.id.greetingBanner);
        Button btnCloseBanner = findViewById(R.id.btnCloseBanner);
        Button btnSettings = findViewById(R.id.btnSettings);

        btnCloseBanner.setOnClickListener(v -> greetingBanner.setVisibility(View.GONE));

        btnSettings.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
        });

        btnConnect.setOnClickListener(v -> {
            if (!isConnected) {
                SharedPreferences sharedPref = getSharedPreferences("VpnConfig", Context.MODE_PRIVATE);
                String savedLink = sharedPref.getString("ss_link", "");

                if (savedLink == null || savedLink.trim().isEmpty()) {
                    Toast.makeText(MainActivity.this, "Please copy, paste or scan a valid Shadowsocks link first!", Toast.LENGTH_LONG).show();
                    return;
                }

                Intent intent = VpnService.prepare(MainActivity.this);
                if (intent != null) {
                    vpnPermissionLauncher.launch(intent);
                } else {
                    handleVpnConnectionSuccess();
                }
            } else {
                handleVpnDisconnection();
            }
        });
    }

    private void handleVpnConnectionSuccess() {
        startVpnService();
        Button btnConnect = findViewById(R.id.btnConnect);
        btnConnect.setText("DISCONNECT");
        btnConnect.setBackgroundResource(R.drawable.btn_connected_glow);
        Toast.makeText(this, "VPN Connected Successfully!", Toast.LENGTH_SHORT).show();
        isConnected = true;
    }

    private void handleVpnDisconnection() {
        stopVpnService();
        Button btnConnect = findViewById(R.id.btnConnect);
        btnConnect.setText("CONNECT\nENGINE");
        btnConnect.setBackgroundResource(R.drawable.btn_disconnected_glow);
        Toast.makeText(this, "VPN Disconnected", Toast.LENGTH_SHORT).show();
        isConnected = false;
    }

    private void startVpnService() {
        SharedPreferences sharedPref = getSharedPreferences("VpnConfig", Context.MODE_PRIVATE);
        String savedLink = sharedPref.getString("ss_link", "");

        Intent intent = new Intent(this, ShadowsocksVpnService.class);
        intent.putExtra("SERVER_LINK", savedLink);
        ContextCompat.startForegroundService(this, intent);
    }

    private void stopVpnService() {
        Intent intent = new Intent(this, ShadowsocksVpnService.class);
        intent.setAction("STOP_VPN");
        stopService(intent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 101) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("WhatsAppVPN", "Notification permission granted");
            }
        }
    }
}
