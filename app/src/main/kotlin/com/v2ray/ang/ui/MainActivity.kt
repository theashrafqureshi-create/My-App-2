package com.v2ray.ang.ui

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.v2ray.ang.R

class MainActivity : AppCompatActivity() {

    private var isConnected = false
    private lateinit var btnConnect: Button
    private lateinit var tvStatus: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnConnect = findViewById(R.id.btnConnect)
        tvStatus = findViewById(R.id.tvStatus)

        // बटन को एकदम गोल (Round) और सुंदर लुक देने के लिए डिज़ाइन
        updateButtonUI()

        // 1. ऐप खुलते ही वेलकम पॉप-अप दिखाना
        showWelcomeDialog()

        // 2. कनेक्ट बटन का क्लिक लॉजिक
        btnConnect.setOnClickListener {
            handleConnectClick()
        }
    }

    // वेलकम पॉप-अप का फंक्शन (शर्त के मुताबिक OK और Cancel के साथ)
    private fun showWelcomeDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Welcome")
        builder.setMessage(getString(R.string.welcome_message))
        builder.setCancelable(false) // जब तक बटन न दबाए, पॉप-अप न हटे
        
        builder.setPositiveButton("OK") { dialog, _ ->
            dialog.dismiss()
        }
        
        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.dismiss()
            finish() // अगर कैंसल दबाए तो ऐप बंद हो जाए
        }

        val dialog = builder.create()
        dialog.show()
    }

    // कनेक्ट बटन दबाने पर क्या होगा
    private fun handleConnectClick() {
        val sharedPreferences = getSharedPreferences("VPN_SETTINGS", Context.MODE_PRIVATE)
        val serverLink = sharedPreferences.getString("SERVER_LINK", null)

        if (serverLink.isNullOrEmpty()) {
            // शर्त के मुताबिक: अगर लिंक नहीं जुड़ी है, तो स्कैन या पेस्ट करने को कहें
            Toast.makeText(this, "No server link found! Please scan QR or paste link in Settings.", Toast.LENGTH_LONG).show()
            // यहाँ आगे जाकर हम स्कैनर या सेटिंग्स स्क्रीन खोलेंगे
        } else {
            // अगर लिंक पहले से मौजूद है, तो सीधे कनेक्ट/डिस्कनेक्ट करें
            if (isConnected) {
                isConnected = false
                tvStatus.text = "Disconnected"
                tvStatus.setTextColor(Color.parseColor("#7F8C8D"))
                updateButtonUI()
            } else {
                isConnected = true
                tvStatus.text = "Connected to WhatsApp VPN"
                tvStatus.setTextColor(Color.parseColor("#075E54"))
                updateButtonUI()
            }
        }
    }

    // बटन का रंग और गोलाई बदलने के लिए
    private fun updateButtonUI() {
        val shape = GradientDrawable()
        shape.shape = GradientDrawable.OVAL // बटन को गोल बनाएगा
        
        if (isConnected) {
            shape.setColor(Color.parseColor("#E74C3C")) // कनेक्ट होने पर बटन का रंग लाल (Disconnect के लिए)
            btnConnect.text = "DISCONNECT"
        } else {
            shape.setColor(Color.parseColor("#075E54")) // डिस्कनेक्ट होने पर व्हाट्सएप वाला हरा रंग
            btnConnect.text = "CONNECT"
        }
        
        btnConnect.background = shape
    }
}
