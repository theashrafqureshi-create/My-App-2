package com.ashraf.whatsappvpn.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.ashraf.whatsappvpn.R
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import java.io.InputStream

class SettingsActivity : AppCompatActivity() {

    // लाइव कैमरा स्कैनर लॉन्चर
    private val barcodeLauncher = registerForActivityResult(ScanContract()) { result ->
        if (result.contents == null) {
            Toast.makeText(this, "Cancelled Scanner", Toast.LENGTH_SHORT).show()
        } else {
            findViewById<EditText>(R.id.etVpnLink).setText(result.contents)
            Toast.makeText(this, "Scanned Configuration Loaded!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val btnBack = findViewById<TextView>(R.id.btnBack)
        val etVpnLink = findViewById<EditText>(R.id.etVpnLink)
        val btnSaveLink = findViewById<Button>(R.id.btnSaveLink)
        val btnOpenScanner = findViewById<Button>(R.id.btnOpenScanner)
        val btnOpenGallery = findViewById<Button>(R.id.btnOpenGallery)

        btnBack.setOnClickListener { finish() }

        btnSaveLink.setOnClickListener {
            val link = etVpnLink.text.toString().trim()
            if (link.startsWith("ss://")) {
                Toast.makeText(this, "Shadowsocks Config Saved!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Error: Invalid Shadowsocks Link!", Toast.LENGTH_SHORT).show()
            }
        }

        // लाइव कैमरा स्कैनर बटन एक्शन
        btnOpenScanner.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 101)
            } else {
                startQrCameraEngine()
            }
        }

        // गैलरी बटन एक्शन
        btnOpenGallery.setOnClickListener {
            val storagePermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Manifest.permission.READ_MEDIA_IMAGES
            } else {
                Manifest.permission.READ_EXTERNAL_STORAGE
            }

            if (ContextCompat.checkSelfPermission(this, storagePermission) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(storagePermission), 102)
            } else {
                openGalleryPicker()
            }
        }
    }

    private fun startQrCameraEngine() {
        val options = ScanOptions().apply {
            setDesiredBarcodeFormats(ScanOptions.QR_CODE)
            setPrompt("Align Shadowsocks QR Code inside the box to Scan")
            setCameraId(0)
            setBeepEnabled(true)
            setBarcodeImageEnabled(true)
            setOrientationLocked(false)
        }
        barcodeLauncher.launch(options)
    }

    private fun openGalleryPicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, 103)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (requestCode == 101) startQrCameraEngine()
            if (requestCode == 102) openGalleryPicker()
        } else {
            Toast.makeText(this, "Permission Denied!", Toast.LENGTH_SHORT).show()
        }
    }

    // गैलरी से चुनी हुई फोटो (Bitmap) से QR कोड डिकोड करने का असली इंजन
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && requestCode == 103 && data != null) {
            val imageUri: Uri? = data.data
            if (imageUri != null) {
                try {
                    val imageStream: InputStream? = contentResolver.openInputStream(imageUri)
                    val bitmap = BitmapFactory.decodeStream(imageStream)
                    
                    val width = bitmap.width
                    val height = bitmap.height
                    val pixels = IntArray(width * height)
                    bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
                    
                    val source = RGBLuminanceSource(width, height, pixels)
                    val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
                    val reader = MultiFormatReader()
                    
                    val result = reader.decode(binaryBitmap)
                    findViewById<EditText>(R.id.etVpnLink).setText(result.text)
                    Toast.makeText(this, "QR Code Processed from Gallery!", Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                    Toast.makeText(this, "Failed to parse QR Code from this image!", Toast.LENGTH_LONG).show()
                    e.printStackTrace()
                }
            }
        }
    }
}
