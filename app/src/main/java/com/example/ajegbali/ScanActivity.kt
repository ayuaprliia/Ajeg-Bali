package com.example.ajegbali

import android.Manifest
import android.content.Intent // PENTING: Import ini ditambahkan
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ScanActivity : AppCompatActivity() {

    private lateinit var viewFinder: androidx.camera.view.PreviewView
    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService

    // Default pakai kamera belakang
    private var cameraSelector: CameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

    // --- 1. SETUP PELUNCUR GALERI (LAUNCHER) ---
    // Fungsi ini menangani apa yang terjadi setelah user memilih foto dari galeri
    private val startGallery = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            // Jika foto dipilih, langsung kirim ke halaman Result
            moveToResult(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scan)

        // 2. Hubungkan Komponen
        viewFinder = findViewById(R.id.viewFinder)
        val btnCapture = findViewById<CardView>(R.id.btnCapture)
        val btnSwitch = findViewById<CardView>(R.id.btnSwitchCamera)
        val btnGallery = findViewById<CardView>(R.id.btnGallery)
        val btnBack = findViewById<ImageView>(R.id.btnBack)

        // 3. Cek Izin Kamera
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }

        // 4. Setup Tombol Capture (Ambil Foto)
        btnCapture.setOnClickListener {
            takePhoto()
        }

        // 5. Setup Tombol Switch Camera (Ganti Depan/Belakang)
        btnSwitch.setOnClickListener {
            cameraSelector = if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
                CameraSelector.DEFAULT_FRONT_CAMERA
            } else {
                CameraSelector.DEFAULT_BACK_CAMERA
            }
            startCamera() // Restart kamera
        }

        // 6. Setup Tombol Gallery (Buka Galeri HP)
        btnGallery.setOnClickListener {
            // Perintah: Buka file picker khusus gambar
            startGallery.launch("image/*")
        }

        // 7. Setup Tombol Back (Kembali ke Home)
        btnBack.setOnClickListener {
            finish()
        }

        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            // Mengikat lifecycle kamera ke Activity ini
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview (Tampilan Layar)
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(viewFinder.surfaceProvider)
                }

            // ImageCapture (Untuk ambil foto)
            imageCapture = ImageCapture.Builder().build()

            try {
                // Lepaskan use case sebelumnya sebelum mengikat yang baru
                cameraProvider.unbindAll()

                // Ikat use cases ke kamera
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture
                )

            } catch (exc: Exception) {
                Log.e(TAG, "Gagal memunculkan kamera.", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun takePhoto() {
        // Pastikan use case imageCapture sudah siap
        val imageCapture = imageCapture ?: return

        // Buat nama file unik berdasarkan waktu
        val name = SimpleDateFormat(FILENAME_FORMAT, Locale.US)
            .format(System.currentTimeMillis())

        // Tempat penyimpanan sementara (Cache)
        val photoFile = File(externalCacheDir, "$name.jpg")

        // Setup Output Options
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        // Ambil Foto!
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Gagal mengambil foto: ${exc.message}", exc)
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val msg = "Foto berhasil diambil!"
                    Log.d(TAG, msg)

                    // Buat URI dari file yang baru saja disimpan
                    val savedUri = Uri.fromFile(photoFile)

                    // Jalankan di UI Thread agar aman saat pindah activity
                    runOnUiThread {
                        Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()

                        // --- LANGKAH PENTING: Pindah ke ResultActivity ---
                        moveToResult(savedUri)
                    }
                }
            }
        )
    }

    // Fungsi untuk pindah ke halaman Hasil (ResultActivity)
    // Fungsi ini menerima URI (alamat file foto) dan mengirimnya ke halaman sebelah
    private fun moveToResult(uri: Uri) {
        val intent = Intent(this, ResultActivity::class.java)
        intent.putExtra("image_uri", uri.toString())
        startActivity(intent)
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(this, "Izin kamera tidak diberikan.", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    companion object {
        private const val TAG = "CameraXApp"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }
}