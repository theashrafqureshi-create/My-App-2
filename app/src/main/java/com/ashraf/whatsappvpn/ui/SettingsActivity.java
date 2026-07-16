package com.ashraf.whatsappvpn.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.ashraf.whatsappvpn.R;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        TextView btnBack = findViewById(R.id.btnBack);
        
        // 🛠️ 5 मैनुअल इनपुट बॉक्स और ड्रॉपडाउन (Spinner) के लिए विजेट्स
        EditText etClientName = findViewById(R.id.etClientName);
        EditText etServerIp = findViewById(R.id.etServerIp);
        EditText etServerPort = findViewById(R.id.etServerPort);
        EditText etPassword = findViewById(R.id.etPassword);
        Spinner spEncryptionMethod = findViewById(R.id.spEncryptionMethod);
        
        Button btnSaveLink = findViewById(R.id.btnSaveLink); // यह आपका सेव बटन ही रहेगा

        // 🛠️ मेमोरी से पुराना सेव किया हुआ डेटा लोड करना (अगर पहले से सेव है)
        SharedPreferences sharedPref = getSharedPreferences("VpnSettings", Context.MODE_PRIVATE);
        
        etClientName.setText(sharedPref.getString("CLIENT_NAME", ""));
        etServerIp.setText(sharedPref.getString("SERVER_IP", ""));
        
        int savedPort = sharedPref.getInt("SERVER_PORT", 8388);
        etServerPort.setText(String.valueOf(savedPort));
        
        etPassword.setText(sharedPref.getString("PASSWORD", ""));

        // 🛠️ फिक्स: ड्रॉपडाउन (Spinner) में एन्क्रिप्शन मेथड सेट करने का सुरक्षित तरीका
        String savedMethod = sharedPref.getString("ENCRYPTION_METHOD", "aes-256-gcm");
        if (spEncryptionMethod != null && spEncryptionMethod.getAdapter() != null) {
            try {
                ArrayAdapter<CharSequence> adapter = (ArrayAdapter<CharSequence>) spEncryptionMethod.getAdapter();
                int spinnerPosition = adapter.getPosition(savedMethod);
                if (spinnerPosition >= 0) {
                    spEncryptionMethod.setSelection(spinnerPosition);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        btnBack.setOnClickListener(v -> finish());

        // 🛠️ सेव बटन दबाने पर सभी 5 वैल्यूज को SharedPreferences में स्टोर करना
        btnSaveLink.setOnClickListener(v -> {
            String clientName = etClientName.getText().toString().trim();
            String serverIp = etServerIp.getText().toString().trim();
            String portStr = etServerPort.getText().toString().trim();
            String password = etPassword.getText().toString().trim();
            
            String method = "aes-256-gcm";
            if (spEncryptionMethod != null && spEncryptionMethod.getSelectedItem() != null) {
                method = spEncryptionMethod.getSelectedItem().toString();
            }

            // वैलिडेशन: चेक करें कि कोई मुख्य बॉक्स खाली तो नहीं है
            if (serverIp.isEmpty() || portStr.isEmpty() || password.isEmpty()) {
                Toast.makeText(SettingsActivity.this, "Error: IP, Port, and Password are required!", Toast.LENGTH_SHORT).show();
                return;
            }

            int port;
            try {
                port = Integer.parseInt(portStr);
            } catch (NumberFormatException e) {
                Toast.makeText(SettingsActivity.this, "Error: Invalid Port Number!", Toast.LENGTH_SHORT).show();
                return;
            }

            // 💾 डेटा को 'VpnSettings' के अंदर सेव करना
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putString("CLIENT_NAME", clientName);
            editor.putString("SERVER_IP", serverIp);
            editor.putInt("SERVER_PORT", port);
            editor.putString("PASSWORD", password);
            editor.putString("ENCRYPTION_METHOD", method);
            editor.apply();

            Toast.makeText(SettingsActivity.this, "VPN Configuration Saved Successfully! ✅", Toast.LENGTH_SHORT).show();
            finish(); // सेव होने के बाद ऑटोमैटिकली होम स्क्रीन पर वापस भेज देगा
        });
    }
}
