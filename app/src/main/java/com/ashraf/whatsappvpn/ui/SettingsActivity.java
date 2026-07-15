package com.ashraf.whatsappvpn.ui;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

// 🎯 [CRITICAL FIX] सही R क्लास इम्पोर्ट
import com.ashraf.whatsappvpn.R;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import java.io.InputStream;

public class SettingsActivity extends AppCompatActivity {

    private final ActivityResultLauncher<ScanOptions> barcodeLauncher = registerForActivityResult(
            new ScanContract(),
            result -> {
                if (result.getContents() == null) {
                    Toast.makeText(SettingsActivity.this, "Cancelled Scanner", Toast.LENGTH_SHORT).show();
                } else {
                    String scannedText = result.getContents();
                    EditText etVpnLink = findViewById(R.id.etVpnLink);
                    etVpnLink.setText(scannedText);
                    saveVpnConfig(scannedText);
                    Toast.makeText(SettingsActivity.this, "Scanned Configuration Loaded!", Toast.LENGTH_SHORT).show();
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        TextView btnBack = findViewById(R.id.btnBack);
        EditText etVpnLink = findViewById(R.id.etVpnLink);
        Button btnSaveLink = findViewById(R.id.btnSaveLink);
        Button btnOpenScanner = findViewById(R.id.btnOpenScanner);
        Button btnOpenGallery = findViewById(R.id.btnOpenGallery);

        SharedPreferences sharedPref = getSharedPreferences("VpnConfig", Context.MODE_PRIVATE);
        String savedLink = sharedPref.getString("ss_link", "");
        if (savedLink != null && !savedLink.isEmpty()) {
            etVpnLink.setText(savedLink);
        }

        btnBack.setOnClickListener(v -> finish());

        btnSaveLink.setOnClickListener(v -> {
            String link = etVpnLink.getText().toString().trim();
            if (link.startsWith("ss://")) {
                saveVpnConfig(link);
                Toast.makeText(SettingsActivity.this, "Shadowsocks Config Saved!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(SettingsActivity.this, "Error: Invalid Shadowsocks Link!", Toast.LENGTH_SHORT).show();
            }
        });

        btnOpenScanner.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(SettingsActivity.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(SettingsActivity.this, new String[]{Manifest.permission.CAMERA}, 101);
            } else {
                startQrCameraEngine();
            }
        });

        btnOpenGallery.setOnClickListener(v -> {
            String storagePermission;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                storagePermission = Manifest.permission.READ_MEDIA_IMAGES;
            } else {
                storagePermission = Manifest.permission.READ_EXTERNAL_STORAGE;
            }

            if (ContextCompat.checkSelfPermission(SettingsActivity.this, storagePermission) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(SettingsActivity.this, new String[]{storagePermission}, 102);
            } else {
                openGalleryPicker();
            }
        });
    }

    private void saveVpnConfig(String link) {
        SharedPreferences sharedPref = getSharedPreferences("VpnConfig", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString("ss_link", link);
        editor.apply();
    }

    private void startQrCameraEngine() {
        ScanOptions options = new ScanOptions();
        options.setDesiredBarcodeFormats(ScanOptions.QR_CODE);
        options.setPrompt("Align Shadowsocks QR Code inside the box to Scan");
        options.setCameraId(0);
        options.setBeepEnabled(true);
        options.setBarcodeImageEnabled(true);
        options.setOrientationLocked(false);
        barcodeLauncher.launch(options);
    }

    private void openGalleryPicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, 103);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (requestCode == 101) startQrCameraEngine();
            if (requestCode == 102) openGalleryPicker();
        } else {
            Toast.makeText(this, "Permission Denied!", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && requestCode == 103 && data != null) {
            Uri imageUri = data.getData();
            if (imageUri != null) {
                try {
                    InputStream imageStream = getContentResolver().openInputStream(imageUri);
                    Bitmap bitmap = BitmapFactory.decodeStream(imageStream);

                    int width = bitmap.getWidth();
                    int height = bitmap.getHeight();
                    int[] pixels = new int[width * height];
                    bitmap.getPixels(pixels, 0, width, 0, 0, width, height);

                    RGBLuminanceSource source = new RGBLuminanceSource(width, height, pixels);
                    BinaryBitmap binaryBitmap = new BinaryBitmap(new HybridBinarizer(source));
                    MultiFormatReader reader = new MultiFormatReader();

                    Result result = reader.decode(binaryBitmap);
                    String scannedText = result.getText();

                    EditText etVpnLink = findViewById(R.id.etVpnLink);
                    etVpnLink.setText(scannedText);
                    saveVpnConfig(scannedText);
                    Toast.makeText(this, "QR Code Processed from Gallery!", Toast.LENGTH_LONG).show();
                } catch (Exception e) {
                    Toast.makeText(this, "Failed to parse QR Code from this image!", Toast.LENGTH_LONG).show();
                    e.printStackTrace();
                }
            }
        }
    }
}
