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
import android.util.Log
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

    // 🎯 Android 14 के लिए सुरक्षित परमिशन लॉन्चर (फिक्स्ड रिस्पॉन्स चेकिंग)
    private val vpnPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        // एंड्रॉइड वीपीएन डायलॉग के बाद दोबारा सुरक्षा जांच करना सबसे बेस्ट तरीका है
        val intent = VpnService.prepare(this)
        if (intent == null) {
            // अगर दोबारा चेक करने पर इंटेंट नल है, मतलब परमिशन पक्का मिल चुकी है
            Handler(Looper.getMainLooper()).postDelayed({
                handleVpnConnectionSuccess()
            }, 200)
        } else {
            Toast.makeText(this, "VPN Permission Required to Connect!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }

        val btnConnect = findViewById<Button>(R.id.btnConnect)
        val greetingBanner = findViewById<RelativeLayout>(R.id.greetingBanner)
        val btnCloseBanner = findViewById<Button>(R.id.btnCloseBanner)
        val btnSettings = findViewById<Button>(R.id.btnSettings)

        btnCloseBanner.setOnClickListener {
            greetingBanner.visibility = View.GONE
        }

        btnSettings.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }

        // वीपीएन कनेक्ट बटन का लॉजिक
        btnConnect.setOnClickListener {
            if (!isConnected) {
                // 🎯 [SMART VALIDATION] सर्विस चालू करने से पहले चेक करो कि क्या लिंक सच में सेव है
                val sharedPref = getSharedPreferences("VpnConfig", Context.MODE_PRIVATE)
                val savedLink = sharedPref.getString("ss_link", "")

                if (savedLink.isNullOrBlank()) {
                    // अगर लिंक नहीं है, तो सर्विस स्टार्ट मत करो, सीधे स्क्रीन पर बैनर अलर्ट टोस्ट दिखाओ
                    Toast.makeText(this, "Please copy, paste or scan a valid Shadowsocks link first!", Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                }

                // सुरक्षित तरीके से वीपीएन परमिशन की जांच
                val intent = VpnService.prepare(this)
                if (intent != null) {
                    vpnPermissionLauncher.launch(intent)
                } else {
                    handleVpnConnectionSuccess()
                }
            } else {
                handleVpnDisconnection()
            }
        }
    }

    private fun handleVpnConnectionSuccess() {
        startVpnService()
        val btnConnect = findViewById<Button>(R.id.btnConnect)
        btnConnect.text = "DISCONNECT"
        btnConnect.setBackgroundResource(R.drawable.btn_connected_glow)
        Toast.makeText(this, "VPN Connected Successfully!", Toast.LENGTH_SHORT).show()
        isConnected = true
    }

    private fun handleVpnDisconnection() {
        stopVpnService()
        val btnConnect = findViewById<Button>(R.id.btnConnect)
        btnConnect.text = "CONNECT\nENGINE"
        btnConnect.setBackgroundResource(R.drawable.btn_disconnected_glow)
        Toast.makeText(this, "VPN Disconnected", Toast.LENGTH_SHORT).show()
        isConnected = false
    }

    private fun startVpnService() {
        // 🎯 [FIXED] कोट्लिन के लिए सही सिंटैक्स
        val intent = Intent(this, ShadowsocksVpnService::class.java)
        ContextCompat.startForegroundService(this, intent)
    }

    private fun stopVpnService() {
        // 🎯 [FIXED] कोट्लिन के लिए सही सिंटैक्स
        val intent = Intent(this, ShadowsocksVpnService::class.java)
        // 🎯 सर्विस को साफ़-साफ़ बंद होने का एक्शन भेजना
        intent.action = "STOP_VPN"
        stopService(intent)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 101) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("WhatsAppVPN", "Notification permission granted")
            }
        }
    }
}
