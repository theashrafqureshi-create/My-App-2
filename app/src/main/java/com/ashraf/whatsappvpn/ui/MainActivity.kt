- name: Inject Clean and Safe UI Interface Controller
 run: |
 mkdir -p app/src/main/kotlin/com/v2ray/ang/ui
 cat << 'EOF' > app/src/main/kotlin/com/v2ray/ang/ui/MainActivity.kt
 package com.v2ray.ang.ui
 import android.content.Intent
 import android.net.VpnService
 import android.os.Bundle
 import android.widget.Button
 import android.widget.TextView
 import android.widget.Toast
 import androidx.appcompat.app.AppCompatActivity
 import com.v2ray.ang.service.RealVpnCore
 class MainActivity : AppCompatActivity() {
 private val vpnRequestCode = 2026
 private lateinit var statusTextView: TextView
 override fun onCreate(savedInstanceState: Bundle?) {
 super.onCreate(savedInstanceState)
 // Dynamically initializing layout to prevent layout mismatch errors
 val rootLayout = android.widget.LinearLayout(this).apply {
 orientation = android.widget.LinearLayout.VERTICAL
 gravity = android.view.Gravity.CENTER padding = 32
 setBackgroundColor(0xFFFAFAFA.toInt())
 }
 statusTextView = TextView(this).apply {
 text = "Status: Disconnected"
 textSize = 18f
 setTextColor(0xFF7F8C8D.toInt())
 setPadding(0, 0, 0, 48)
 }
 val connectButton = Button(this).apply {
 text = "Connect Secure Bridge"
 setBackgroundColor(0xFF075E54.toInt())
 setTextColor(0xFFFFFFFF.toInt())
 setPadding(24, 16, 24, 16)
 }
 connectButton.setOnClickListener {
 startVpnEngine()
 }
 rootLayout.addView(statusTextView)
 rootLayout.addView(connectButton)
 setContentView(rootLayout)
 }
 private fun startVpnEngine() {
 val intent = VpnService.prepare(this)
 if (intent != null) {
 startActivityForResult(intent, vpnRequestCode)
 } else {
 onActivityResult(vpnRequestCode, RESULT_OK, null)
 }
 }
 override fun onActivityResult(requestCode: Int, resultCode: Int, data: 
Intent?) {
 super.onActivityResult(requestCode, resultCode, data)
 if (requestCode == vpnRequestCode && resultCode == RESULT_OK) {
 val intent = Intent(this, RealVpnCore::class.java)
 if (android.os.Build.VERSION.SDK_INT >= 
android.os.Build.VERSION_CODES.O) {
 startForegroundService(intent)
 } else {
 startService(intent)
 }
 statusTextView.text = "Status: Connected & Protected"
 statusTextView.setTextColor(0xFF128C7E.toInt())
 Toast.makeText(this, "Secure Tunnel Activated!", 
Toast.LENGTH_SHORT).show()
 } else {
 Toast.makeText(this, "Permission Denied by User", 
Toast.LENGTH_SHORT).show() }
 }
 }
 EOF
