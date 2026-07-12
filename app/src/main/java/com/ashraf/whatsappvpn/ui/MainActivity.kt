package com.ashraf.whatsappvpn.ui

import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.ashraf.whatsappvpn.R
import com.ashraf.whatsappvpn.service.ShadowsocksVpnService

class MainActivity : AppCompatActivity() {

    private var isConnected = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btnConnect = findViewById<Button>(R.id.btnConnect)
        val greetingBanner = findViewById<RelativeLayout>(R.id.greetingBanner)
        val btnCloseBanner = findViewById<Button>(R.id.btnCloseBanner)
        val btnSettings = findViewById<Button>(R.id.btnSettings)

        // बैनर बंद करने का लॉजिक
        btnCloseBanner.setOnClickListener {
            greetingBanner.visibility = View.GONE
        }

        // हमने यह नया कमांड जोड़ा: सेटिंग्स गियर बटन पर क्लिक करने पर सेटिंग्स पेज खुलेगा
        btnSettings.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }

        // वीपीएन कनेक्ट बटन का लॉजिक
        btnConnect.setOnClickListener {
            if (!isConnected) {
                val intent = VpnService.prepare(this)
                if (intent != null) {
                    startActivityForResult(intent, 0)
                } else {
                    onActivityResult(0, RESULT_OK, null)
                }
            } else {
                stopVpnService()
                btnConnect.text = "CONNECT\nENGINE"
                btnConnect.setBackgroundResource(R.drawable.btn_disconnected_glow)
                Toast.makeText(this, "VPN Disconnected", Toast.LENGTH_SHORT).show()
                isConnected = false
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            startVpnService()
            val btnConnect = findViewById<Button>(R.id.btnConnect)
            btnConnect.text = "DISCONNECT"
            btnConnect.setBackgroundResource(R.drawable.btn_connected_glow)
            Toast.makeText(this, "VPN Connected Successfully!", Toast.LENGTH_SHORT).show()
            isConnected = true
        }
    }

    private fun startVpnService() {
        val intent = Intent(this, ShadowsocksVpnService::class.java).apply {
            action = "START"
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private fun stopVpnService() {
        val intent = Intent(this, ShadowsocksVpnService::class.java).apply {
            action = "STOP"
        }
        startService(intent)
    }
}
