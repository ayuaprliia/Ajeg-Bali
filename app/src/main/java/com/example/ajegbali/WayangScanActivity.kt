package com.example.ajegbali

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat

import com.example.ajegbali.WayangDetectionSmoother
import com.example.ajegbali.WayangDetector
import com.example.ajegbali.WayangImageProcessor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Activity untuk real-time wayang detection menggunakan CameraX.
 * Menampilkan bounding box dengan confidence score.
 * Support capture foto dan upload dari galeri.
 */
class WayangScanActivity : AppCompatActivity() {

    private lateinit var previewView: PreviewView
    private lateinit var boundingBoxView: WayangBoundingBoxView
    private lateinit var progressBar: ProgressBar
    private lateinit var statusText: TextView
    private lateinit var btnCapture: CardView
    private lateinit var btnGallery: CardView
    private lateinit var btnSwitchCamera: CardView
    private lateinit var btnBack: ImageView

    private var detector: WayangDetector? = null
    private val smoother = WayangDetectionSmoother()
    private var cameraExecutor: ExecutorService? = null
    private var isProcessing = false

    // Variabel state untuk menyimpan frame terakhir
    private var lastCapturedBitmap: Bitmap? = null
    private var latestDetections: List<WayangDetectionResult> = emptyList()

    // Camera selector untuk flip kamera
    private var cameraSelector: CameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

    // Proteksi race condition saat memproses frame & onDestroy
    private val detectorLock = Any()
    private var isDestroyedFlag = false

    companion object {
        const val TAG = "WayangScan"
        const val CONFIDENCE_THRESHOLD = 0.50f
        const val IOU_THRESHOLD = 0.45f
    }

    // 1. DEKLARASI LAUNCHER IZIN KAMERA DI SINI (Level Class)
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startCamera()
        } else {
            Toast.makeText(this, "Izin kamera diperlukan", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    // 2. DEKLARASI LAUNCHER GALERI DI SINI (Level Class)
    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.data?.let { uri ->
                processGalleryImage(uri)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wayang_scan)

        // Initialize UI
        previewView = findViewById(R.id.previewView)
        previewView.scaleType = PreviewView.ScaleType.FIT_CENTER
        boundingBoxView = findViewById(R.id.boundingBoxView)
        progressBar = findViewById(R.id.progressBar)
        statusText = findViewById(R.id.statusText)
        btnCapture = findViewById(R.id.btnCapture)
        btnGallery = findViewById(R.id.btnGallery)
        btnSwitchCamera = findViewById(R.id.btnSwitchCamera)
        btnBack = findViewById(R.id.btnBack)

        // Initialize detector
        try {
            detector = WayangDetector(this)
            statusText.text = "Model siap"
        } catch (e: Exception) {
            Log.e(TAG, "Gagal init detector: ${e.message}")
            statusText.text = "Gagal memuat model"
            return
        }

        cameraExecutor = Executors.newSingleThreadExecutor()

        // Setup button listeners
        btnCapture.setOnClickListener { capturePhoto() }
        btnGallery.setOnClickListener { selectFromGallery() }
        btnSwitchCamera.setOnClickListener {
            cameraSelector = if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
                CameraSelector.DEFAULT_FRONT_CAMERA
            } else {
                CameraSelector.DEFAULT_BACK_CAMERA
            }
            startCamera()
        }
        btnBack.setOnClickListener { finish() }

        // Request camera permission dengan aman
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also { analysis ->
                    analysis.setAnalyzer(cameraExecutor!!) { imageProxy ->
                        processFrame(imageProxy)
                    }
                }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,
                    preview,
                    imageAnalysis
                )
            } catch (e: Exception) {
                Log.e(TAG, "Camera binding error: ${e.message}")
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun processFrame(imageProxy: ImageProxy) {
        if (isProcessing || detector == null) {
            imageProxy.close()
            return
        }
        isProcessing = true

        CoroutineScope(Dispatchers.Default).launch {
            try {
                val bitmap = WayangImageProcessor.imageProxyToBitmap(imageProxy)
                imageProxy.close()

                val startTime = System.currentTimeMillis()
                val rawResults = synchronized(detectorLock) {
                    if (isDestroyedFlag || detector == null) {
                        emptyList()
                    } else {
                        detector!!.detect(bitmap, CONFIDENCE_THRESHOLD, IOU_THRESHOLD)
                    }
                }
                val elapsed = System.currentTimeMillis() - startTime

                // Simpan bitmap ke state (thread-safe copy)
                synchronized(this@WayangScanActivity) {
                    lastCapturedBitmap?.recycle()
                    lastCapturedBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, false)
                }

                // Menghaluskan hasil deteksi
                val smoothedResults = smoother.smooth(rawResults)

                // Simpan deteksi ke state agar bisa dibaca oleh tombol capture
                latestDetections = smoothedResults

                if (!isFinishing && !isDestroyed) {
                    runOnUiThread {
                        boundingBoxView.setDetections(smoothedResults, bitmap.width, bitmap.height)
                        val fps = if (elapsed > 0) 1000 / elapsed else 0
                        statusText.text = "FPS: $fps | ${smoothedResults.size} wayang"
                    }
                }
                bitmap.recycle()
            } catch (e: Exception) {
                Log.e(TAG, "Error processing frame: ${e.message}")
                imageProxy.close()
            } finally {
                isProcessing = false
            }
        }
    }

    private fun selectFromGallery() {
        val intent = Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        galleryLauncher.launch(intent) // Gunakan launcher level kelas
    }

    private fun processGalleryImage(uri: Uri) {
        progressBar.visibility = ProgressBar.VISIBLE
        statusText.text = "Memproses gambar..."

        CoroutineScope(Dispatchers.Default).launch {
            try {
                val inputStream = contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()

                if (bitmap != null) {
                    // 1. Simpan bitmap ke file cache DULU sebelum apa pun
                    val photoFile = File(externalCacheDir, "gallery_capture_${System.currentTimeMillis()}.jpg")
                    FileOutputStream(photoFile).use { out ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                    }
                    val photoUri = Uri.fromFile(photoFile)

                    // 2. Recycle bitmap karena sudah disimpan ke file
                    bitmap.recycle()

                    // 3. Pindah ke halaman hasil
                    if (!isFinishing && !isDestroyed) {
                        val intent = Intent(this@WayangScanActivity, WayangResultActivity::class.java).apply {
                            putExtra("image_uri", photoUri.toString())
                        }
                        runOnUiThread {
                            progressBar.visibility = ProgressBar.GONE
                            startActivity(intent)
                            finish() // SANGAT PENTING: Bebaskan memory & TFLite dari scan activity
                        }
                    }
                } else {
                    runOnUiThread {
                        progressBar.visibility = ProgressBar.GONE
                        statusText.text = "Gagal membaca gambar"
                        Toast.makeText(this@WayangScanActivity, "Gagal membaca gambar dari galeri", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Gallery error: ${e.message}")
                if (!isFinishing && !isDestroyed) {
                    runOnUiThread {
                        progressBar.visibility = ProgressBar.GONE
                        statusText.text = "Gagal memproses gambar"
                    }
                }
            }
        }
    }

    private fun capturePhoto() {
        if (latestDetections.isNotEmpty() && lastCapturedBitmap != null) {
            try {
                // Simpan gambar ke cache (thread-safe)
                val bitmapToSave: Bitmap?
                synchronized(this) {
                    bitmapToSave = lastCapturedBitmap?.copy(Bitmap.Config.ARGB_8888, false)
                }

                if (bitmapToSave != null) {
                    val photoFile = File(externalCacheDir, "wayang_capture_${System.currentTimeMillis()}.jpg")
                    FileOutputStream(photoFile).use { out ->
                        bitmapToSave.compress(Bitmap.CompressFormat.JPEG, 90, out)
                    }
                    bitmapToSave.recycle()
                    val photoUri = Uri.fromFile(photoFile)

                    // Kirim ke halaman hasil
                    val intent = Intent(this, WayangResultActivity::class.java).apply {
                        putExtra("image_uri", photoUri.toString())
                    }
                    startActivity(intent)
                    finish() // SANGAT PENTING: Bebaskan memory & TFLite dari scan activity
                }
            } catch (e: Exception) {
                Log.e(TAG, "Gagal menyimpan gambar: ${e.message}")
                Toast.makeText(this, "Gagal memproses gambar", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Arahkan kamera hingga wayang terdeteksi!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun allPermissionsGranted() =
        ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED

    override fun onDestroy() {
        super.onDestroy()
        isDestroyedFlag = true
        synchronized(detectorLock) {
            detector?.close()
            detector = null
        }
        cameraExecutor?.shutdown()
        synchronized(this) {
            lastCapturedBitmap?.recycle()
            lastCapturedBitmap = null
        }
    }
}