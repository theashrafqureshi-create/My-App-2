package com.ashraf.whatsappvpn.ui

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.ashraf.whatsappvpn.R

class DashboardActivity : AppCompatActivity() {

    private lateinit var tvStatus: TextView
    private lateinit var btnConnect: Button
    private lateinit var etV2rayConfig: EditText
    private var isConnected = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        tvStatus = findViewById(R.id.tv_status)
        btnConnect = findViewById(R.id.btn_connect)
        etV2rayConfig = findViewById(R.id.et_v2ray_config)

        btnConnect.setOnClickListener {
            val configLink = etV2rayConfig.text.toString().trim()

            if (!isConnected) {
                if (configLink.isEmpty()) {
                    Toast.makeText(this, "Please paste a valid V2Ray link first!", Toast.LENGTH_SHORT).show()
                } else {
                    tvStatus.text = "Protected"
                    tvStatus.setTextColor(0xFF00E676.toInt())
                    btnConnect.text = "DISCONNECT"
                    etV2rayConfig.isEnabled = false
                    isConnected = true
                    Toast.makeText(this, "WhatsApp Tunnel Connected!", Toast.LENGTH_SHORT).show()
                }
            } else {
                tvStatus.text = "Not Protected"
                tvStatus.setTextColor(0xFFFF5252.toInt())
                btnConnect.text = "CONNECT"
                etV2rayConfig.isEnabled = true
                isConnected = false
                Toast.makeText(this, "VPN Disconnected", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
