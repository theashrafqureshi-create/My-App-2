package com.ashraf.whatsappvpn.ui

import android.content.Context
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.ashraf.whatsappvpn.R

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val btnBack = findViewById<TextView>(R.id.btnBack)
        
        // 🛠️ 5 मैनुअल इनपुट बॉक्स और ड्रॉपडाउन (Spinner) के लिए विजेट्स
        val etClientName = findViewById<EditText>(R.id.etClientName)
        val etServerIp = findViewById<EditText>(R.id.etServerIp)
        val etServerPort = findViewById<EditText>(R.id.etServerPort)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val spEncryptionMethod = findViewById<Spinner>(R.id.spEncryptionMethod)
        
        val btnSaveLink = findViewById<Button>(R.id.btnSaveLink) // आपका सेव बटन

        // 🛠️ मेमोरी से पुराना सेव किया हुआ डेटा लोड करना
        val sharedPref = getSharedPreferences("VpnSettings", Context.MODE_PRIVATE)
        
        etClientName.setText(sharedPref.getString("CLIENT_NAME", ""))
        etServerIp.setText(sharedPref.getString("SERVER_IP", ""))
        
        val savedPort = sharedPref.getInt("SERVER_PORT", 8388)
        etServerPort.setText(savedPort.toString())
        
        etPassword.setText(sharedPref.getString("PASSWORD", ""))

        // 🛠️ ड्रॉपडाउन (Spinner) में पुराना सेव एन्क्रिप्शन मेथड सेट करना
        val savedMethod = sharedPref.getString("ENCRYPTION_METHOD", "aes-256-gcm")
        if (spEncryptionMethod?.adapter != null) {
            try {
                @Suppress("UNCHECKED_CAST")
                val adapter = spEncryptionMethod.adapter as ArrayAdapter<CharSequence>
                val spinnerPosition = adapter.getPosition(savedMethod)
                if (spinnerPosition >= 0) {
                    spEncryptionMethod.setSelection(spinnerPosition)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // बैक बटन का लॉजिक
        btnBack?.setOnClickListener { finish() }

        // 🛠️ सेव बटन दबाने पर सभी वैल्यूज को SharedPreferences में स्टोर करना
        btnSaveLink?.setOnClickListener {
            val clientName = etClientName.text.toString().trim()
            val serverIp = etServerIp.text.toString().trim()
            val portStr = etServerPort.text.toString().trim()
            val password = etPassword.text.toString().trim()
            
            var method = "aes-256-gcm"
            if (spEncryptionMethod?.selectedItem != null) {
                method = spEncryptionMethod.selectedItem.toString()
            }

            // वैलिडेशन: चेक करें कि कोई मुख्य बॉक्स खाली तो नहीं है
            if (serverIp.isEmpty() || portStr.isEmpty() || password.isEmpty()) {
                Toast.makeText(this@SettingsActivity, "Error: IP, Port, and Password are required!", Toast.LENGTH_SHORT).show()
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
            editor.putString("PASSWORD", password)
            editor.putString("ENCRYPTION_METHOD", method)
            
            // 🔄 V2Ray कंपैटिबिलिटी के लिए एक डमी लिंक भी बैकअप में जनरेट करके रख देते हैं
            editor.putString("V2RAY_CONFIG_LINK", "v2ray://$serverIp:$port")
            
            editor.apply()

            Toast.makeText(this@MainActivity, "VPN Configuration Saved Successfully! ✅", Toast.LENGTH_SHORT).show()
            finish() // सेव होने के बाद ऑटोमैटिकली होम स्क्रीन पर वापस भेज देगा
        }
    }
}
