package com.ashraf.whatsappvpn.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.ashraf.whatsappvpn.R
import com.ashraf.whatsappvpn.service.V2RayVpnService
import java.io.PrintWriter
import java.io.StringWriter

class MainActivity : AppCompatActivity() {

    private var isConnected = false

    // VPN परमिशन हैंडलर (कोटलीन स्टाइल में)
    private val vpnPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        val intent = VpnService.prepare(this@MainActivity)
        if (intent == null) {
            Handler(Looper.getMainLooper()).postDelayed({ handleVpnConnectionSuccess() }, 200)
        } else {
            Toast.makeText(this@MainActivity, "VPN Permission Required to Connect!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. क्रैश ट्रैकर (जैसा आपकी पुरानी फाइल में था)
        Thread.setDefaultUncaughtExceptionHandler { _, throwable ->
            val sw = StringWriter()
            val pw = PrintWriter(sw)
            throwable.printStackTrace(pw)
            val stackTrace = sw.toString()

            Handler(Looper.getMainLooper()).post {
                AlertDialog.Builder(this@MainActivity)
                    .setTitle("App Crashed! Error Details:")
                    .setMessage(stackTrace)
                    .setPositiveButton("OK") { _, _ -> System.exit(1) }
                    .setCancelable(false)
                    .show()
            }
        }

        setContentView(R.layout.activity_main)

        // 2. एंड्रॉइड 13+ के लिए नोटिफिकेशन परमिशन
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }

        // UI एलिमेंट्स को ढूंढना
        val btnConnect = findViewById<Button>(R.id.btnConnect)
        val greetingBanner = findViewById<LinearLayout>(R.id.greetingBanner)
        val btnCloseBanner = findViewById<TextView>(R.id.btnCloseBanner)
        val btnSettings = findViewById<Button>(R.id.btnSettings)

        // बैनर बंद करने का लॉजिक
        btnCloseBanner?.setOnClickListener { greetingBanner?.visibility = View.GONE }

        // सेटिंग्स बटन का लॉजिक
        btnSettings?.setOnClickListener {
            try {
                val intent = Intent()
                intent.setClassName(packageName, "com.ashraf.whatsappvpn.ui.SettingsActivity")
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }

        // कनेक्ट बटन का लॉजिक (अब यह V2Ray की तरफ इशारा करेगा)
        btnConnect?.setOnClickListener {
            if (!isConnected) {
                val sharedPref = getSharedPreferences("VpnSettings", Context.MODE_PRIVATE)
                val configLink = sharedPref.getString("V2RAY_CONFIG_LINK", "")

                // डमी चेक: अगर कॉन्फ़िगरेशन खाली है
                if (configLink.isNullOrBlank()) {
                    Toast.makeText(this@MainActivity, "Please set your V2Ray configuration first!", Toast.LENGTH_LONG).show()
                    try {
                        val intent = Intent()
                        intent.setClassName(packageName, "com.ashraf.whatsappvpn.ui.SettingsActivity")
                        startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(this@MainActivity, "Error opening Settings: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                    return@setOnClickListener
                }

                // VPN परमिशन चेक करना
                val intent = VpnService.prepare(this@MainActivity)
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
        btnConnect?.apply {
            text = "DISCONNECT"
            setBackgroundColor(Color.parseColor("#D32F2F"))
        }
        Toast.makeText(this, "VPN Connected Successfully!", Toast.LENGTH_SHORT).show()
        isConnected = true
    }

    private fun handleVpnDisconnection() {
        stopVpnService()
        val btnConnect = findViewById<Button>(R.id.btnConnect)
        btnConnect?.apply {
            text = "CONNECT"
            setBackgroundColor(Color.parseColor("#075E54"))
        }
        Toast.makeText(this, "VPN Disconnected", Toast.LENGTH_SHORT).show()
        isConnected = false
    }

    // अब यह हमारे नए V2RayVpnService को स्टार्ट करेगा
    private fun startVpnService() {
        val intent = Intent(this, V2RayVpnService::class.java)
        ContextCompat.startForegroundService(this, intent)
    }

    private fun stopVpnService() {
        val intent = Intent(this, V2RayVpnService::class.java).apply {
            action = "STOP_VPN"
        }
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
