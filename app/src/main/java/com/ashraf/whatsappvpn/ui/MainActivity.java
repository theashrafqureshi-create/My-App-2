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
            try {
                Intent intent = new Intent();
                intent.setClassName(getPackageName(), "com.ashraf.whatsappvpn.ui.SettingsActivity");
                startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(MainActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });

        btnConnect.setOnClickListener(v -> {
            if (!isConnected) {
                SharedPreferences sharedPref = getSharedPreferences("VpnSettings", Context.MODE_PRIVATE);
                String serverIp = sharedPref.getString("SERVER_IP", "");
                String password = sharedPref.getString("PASSWORD", "");

                // 🛠️ नया बदलाव: अगर डेटा खाली है (फर्स्ट टाइम यूजर), तो एरर दिखाने के बजाय सीधे सेटिंग्स पेज पर भेजें
                if (serverIp.trim().isEmpty() || password.trim().isEmpty()) {
                    Toast.makeText(MainActivity.this, "Please set your server configuration first!", Toast.LENGTH_LONG).show();
                    
                    try {
                        Intent intent = new Intent();
                        intent.setClassName(getPackageName(), "com.ashraf.whatsappvpn.ui.SettingsActivity");
                        startActivity(intent);
                    } catch (Exception e) {
                        Toast.makeText(MainActivity.this, "Error opening Settings: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                    return; // यहीं से कोड रुक जाएगा, वीपीएन स्टार्ट नहीं होगा
                }

                // अगर डेटा पहले से मौजूद है (सेकंड टाइम), तो सीधे वीपीएन कनेक्शन प्रोसेस शुरू होगा
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
        SharedPreferences sharedPref = getSharedPreferences("VpnSettings", Context.MODE_PRIVATE);
        String serverIp = sharedPref.getString("SERVER_IP", "");
        int serverPort = sharedPref.getInt("SERVER_PORT", 8388);
        String password = sharedPref.getString("PASSWORD", "");
        String method = sharedPref.getString("ENCRYPTION_METHOD", "aes-256-gcm");
        String clientName = sharedPref.getString("CLIENT_NAME", "");

        Intent intent = new Intent(this, ShadowsocksVpnService.class);
        
        intent.putExtra("SERVER_IP", serverIp);
        intent.putExtra("SERVER_PORT", serverPort);
        intent.putExtra("PASSWORD", password);
        intent.putExtra("ENCRYPTION_METHOD", method);
        intent.putExtra("CLIENT_NAME", clientName);
        
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
