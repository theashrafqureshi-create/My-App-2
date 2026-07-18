package com.ashraf.whatsappvpn.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.ashraf.whatsappvpn.R;

public class DashboardActivity extends AppCompatActivity {

    private TextView tvStatus;
    private Button btnConnect;
    private EditText etV2rayConfig;
    private boolean isConnected = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        tvStatus = findViewById(R.id.tv_status);
        btnConnect = findViewById(R.id.btn_connect);
        etV2rayConfig = findViewById(R.id.et_v2ray_config);

        btnConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String configLink = etV2rayConfig.getText().toString().trim();

                if (!isConnected) {
                    if (configLink.isEmpty()) {
                        Toast.makeText(DashboardActivity.this, "Please paste a valid V2Ray link first!", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    tvStatus.setText("Protected");
                    tvStatus.setTextColor(0xFF00E676); 
                    btnConnect.setText("DISCONNECT");
                    etV2rayConfig.setEnabled(false);
                    isConnected = true;
                    Toast.makeText(DashboardActivity.this, "WhatsApp Tunnel Connected!", Toast.LENGTH_SHORT).show();
                } else {
                    tvStatus.setText("Not Protected");
                    tvStatus.setTextColor(0xFFFF5252); 
                    btnConnect.setText("CONNECT");
                    etV2rayConfig.setEnabled(true);
                    isConnected = false;
                    Toast.makeText(DashboardActivity.this, "VPN Disconnected", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
