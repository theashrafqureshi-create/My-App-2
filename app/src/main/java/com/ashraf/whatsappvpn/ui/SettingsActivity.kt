package com.ashraf.whatsappvpn.ui

import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.ashraf.whatsappvpn.R

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val btnBack = findViewById<TextView>(R.id.btnBack)
        
        // 🚀 सिर्फ V2Ray के लिए जरूरी विजेट्स जो आपकी XML में मौजूद हैं
        val etClientName = findViewById<EditText>(R.id.etClientName)
        val etServerIp = findViewById<EditText>(R.id.etServerIp)
        val etServerPort = findViewById<EditText>(R.id.etServerPort)
        
        val btnSaveLink = findViewById<Button>(R.id.btnSaveLink) // आपका सेव बटन

        // 💾 मेमोरी से पुराना सेव किया हुआ V2Ray डेटा लोड करना
        val sharedPref = getSharedPreferences("VpnSettings", Context.MODE_PRIVATE)
        
        etClientName?.setText(sharedPref.getString("CLIENT_NAME", ""))
        etServerIp?.setText(sharedPref.getString("SERVER_IP", ""))
        
        val savedPort = sharedPref.getInt("SERVER_PORT", 443) // Default V2Ray पोर्ट 443 कर दिया
        etServerPort?.setText(savedPort.toString())

        // बैक बटन का लॉजिक
        btnBack?.setOnClickListener { finish() }

        // 🛠️ सेव बटन दबाने पर V2Ray कॉन्फ़िगरेशन को SharedPreferences में स्टोर करना
        btnSaveLink?.setOnClickListener {
            val clientName = etClientName?.text?.toString()?.trim() ?: ""
            val serverIp = etServerIp?.text?.toString()?.trim() ?: ""
            val portStr = etServerPort?.text?.toString()?.trim() ?: ""

            // वैलिडेशन: चेक करें कि कोई मुख्य बॉक्स खाली तो नहीं है
            if (serverIp.isEmpty() || portStr.isEmpty()) {
                Toast.makeText(this@SettingsActivity, "Error: IP and Port are required!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val port: Int
            try {
                port = portStr.toInt()
            } catch (e: NumberFormatException) {
                Toast.makeText(this@SettingsActivity, "Error: Invalid Port Number!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 💾 डेटा को 'VpnSettings' के अंदर सेव करना
            val editor = sharedPref.edit()
            editor.putString("CLIENT_NAME", clientName)
            editor.putString("SERVER_IP", serverIp)
            editor.putInt("SERVER_PORT", port)
            
            // 🔄 शुद्ध V2Ray कॉन्फ़िगरेशन लिंक जो MainActivity और V2RayVpnService को चाहिए
            editor.putString("V2RAY_CONFIG_LINK", "v2ray://$serverIp:$port")
            
            editor.apply()

            Toast.makeText(this@SettingsActivity, "V2Ray Configuration Saved Successfully! ✅", Toast.LENGTH_SHORT).show()
            finish() // सेव होने के बाद ऑटोमैटिकली होम स्क्रीन पर वापस भेज देगा
        }
    }
}
