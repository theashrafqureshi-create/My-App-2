- name: Inject Clean and Safe UI Interface Controller
  run: |
    mkdir -p app/src/main/java/com/ashraf/whatsappvpn/ui
    cat << 'EOF' > app/src/main/java/com/ashraf/whatsappvpn/ui/MainActivity.kt
    package com.ashraf.whatsappvpn.ui

    import android.content.Intent
    import android.net.VpnService
    import android.os.Bundle
    import android.widget.Button
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
                    btnConnect.text = "Connect"
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
                btnConnect.text = "Disconnect"
                Toast.makeText(this, "VPN Connected Successfully!", Toast.LENGTH_SHORT).show()
                isConnected = true
            }
        }

        private fun startVpnService() {
            val intent = Intent(this, ShadowsocksVpnService::class.java)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
        }

        private fun stopVpnService() {
            val intent = Intent(this, ShadowsocksVpnService::class.java)
            stopService(intent)
        }
    }
    EOF
