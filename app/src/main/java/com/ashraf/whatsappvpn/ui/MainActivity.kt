package com.ashraf.whatsappvpn.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.ashraf.whatsappvpn.R
import com.ashraf.whatsappvpn.service.ShadowsocksVpnService

class MainActivity : AppCompatActivity() {

    private var isConnected = false

    // 🎯 Android 14 के लिए सुरक्षित परमिशन लॉन्चर
    private val vpnPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            // सिस्टम डायलॉग पूरी तरह हटने के लिए 200ms का सेफ डिले
            Handler(Looper.getMainLooper()).postDelayed({
                handleVpnConnectionSuccess()
            }, 200)
        } else {
            Toast.makeText(this, "VPN Permission Denied!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 🎯 Android 13/14 क्रैश फिक्स: ऐप खुलते ही सही मैनिफेस्ट परमिशन स्ट्रिंग के साथ रिक्वेस्ट करना
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }

        val btnConnect = findViewById<Button>(R.id.btnConnect)
        val greetingBanner = findViewById<RelativeLayout>(R.id.greetingBanner)
        val btnCloseBanner = findViewById<Button>(R.id.btnCloseBanner)
        val btnSettings = findViewById<Button>(R.id.btnSettings)

        // बैनर बंद करने का लॉजिक
        btnCloseBanner.setOnClickListener {
            greetingBanner.visibility = View.GONE
        }

        // सेटिंग्स गियर बटन पर क्लिक करने पर सेटिंग्स पेज खुलेगा
        btnSettings.setOnClickListener {
            // नोट: अगर SettingsActivity अलग पैकेज में है तो इम्पोर्ट चेक करें
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }

        // वीपीएन कनेक्ट बटन का लॉजिक
        btnConnect.setOnClickListener {
            if (!isConnected) {
                // सुरक्षित तरीके से वीपीएन परमिशन की जांच
                val intent = VpnService.prepare(this)
                if (intent != null) {
                    // अगर परमिशन नहीं है, तो नए लॉन्चर से डायलॉग खोलो
                    vpnPermissionLauncher.launch(intent)
                } else {
                    // अगर परमिशन पहले से मिली हुई है, तो सीधे चालू करो
                    handleVpnConnectionSuccess()
                }
            } else {
                handleVpnDisconnection()
            }
        }
    }

    // जब परमिशन मिल जाए या पहले से मौजूद हो, तो यूआई अपडेट और सर्विस चालू करने का हैंडलर
    private fun handleVpnConnectionSuccess() {
        startVpnService()
        val btnConnect = findViewById<Button>(R.id.btnConnect)
        btnConnect.text = "DISCONNECT"
        btnConnect.setBackgroundResource(R.drawable.btn_connected_glow)
        Toast.makeText(this, "VPN Connected Successfully!", Toast.LENGTH_SHORT).show()
        isConnected = true
    }

    // वीपीएन बंद करने का हैंडलर
    private fun handleVpnDisconnection() {
        stopVpnService()
        val btnConnect = findViewById<Button>(R.id.btnConnect)
        btnConnect.text = "CONNECT\nENGINE"
        btnConnect.setBackgroundResource(R.drawable.btn_disconnected_glow)
        Toast.makeText(this, "VPN Disconnected", Toast.LENGTH_SHORT).show()
        isConnected = false
    }

    private fun startVpnService() {
        val intent = Intent(this, ShadowsocksVpnService::class.java)
        // Android 14 फ़ोरग्राउंड सर्विस को ContextCompat से ही स्टार्ट करना सबसे सेफ होता है
        ContextCompat.startForegroundService(this, intent)
    }

    private fun stopVpnService() {
        val intent = Intent(this, ShadowsocksVpnService::class.java)
        stopService(intent)
    }

    // नोटिफिकेशन परमिशन के रिज़ल्ट को सेफली हैंडल करने के लिए
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 101) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("WhatsAppVPN", "Notification permission granted")
            }
        }
    }
}
