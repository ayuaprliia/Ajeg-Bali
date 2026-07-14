package com.example.ajegbali.feature.wayang

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
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import com.example.ajegbali.BuildConfig
import com.example.ajegbali.R
import com.example.ajegbali.data.remote.websocket.WayangWebSocketClient
import com.example.ajegbali.ml.wayang.WayangDetectionResult
import com.example.ajegbali.ui.WayangBoundingBoxView
import com.example.ajegbali.utils.ImageUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Activity untuk capture foto wayang dan deteksi real-time menggunakan WebSocket.
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

    private var imageCapture: ImageCapture? = null
    private var imageAnalysis: ImageAnalysis? = null
    private var cameraExecutor: ExecutorService? = null
    private var cameraSelector: CameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

    private var wsClient: WayangWebSocketClient? = null
    private var isWsProcessing = false

    companion object {
        const val TAG = "WayangScan"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
    }

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

    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.data?.let { uri ->
                moveToResult(uri)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wayang_scan)

        // Initialize UI
        previewView = findViewById(R.id.previewView)
        boundingBoxView = findViewById(R.id.boundingBoxView)
        progressBar = findViewById(R.id.progressBar)
        statusText = findViewById(R.id.statusText)
        btnCapture = findViewById(R.id.btnCapture)
        btnGallery = findViewById(R.id.btnGallery)
        btnSwitchCamera = findViewById(R.id.btnSwitchCamera)
        btnBack = findViewById(R.id.btnBack)

        statusText.text = "Mencoba menghubungkan..."

        cameraExecutor = Executors.newSingleThreadExecutor()

        setupWebSocket()

        btnCapture.setOnClickListener { takePhoto() }
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

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun setupWebSocket() {
        // ngrok often uses https -> wss
        val baseUrl = BuildConfig.API_BASE_URL
            .replace("https://", "wss://")
            .replace("http://", "ws://")
            
        val wsUrl = if (baseUrl.endsWith("/")) "${baseUrl}ws/detect-wayang" else "$baseUrl/ws/detect-wayang"
        
        Log.d(TAG, "Initializing WebSocket at: $wsUrl")
        
        wsClient = WayangWebSocketClient(
            url = wsUrl,
            onResult = { detections ->
                runOnUiThread {
                    boundingBoxView.setDetections(detections, 640, 640) // Backend scales to 640
                    statusText.text = if (detections.isNotEmpty()) {
                        "Terdeteksi: ${detections.joinToString { it.characterName }}"
                    } else {
                        "Mencari wayang..."
                    }
                }
                isWsProcessing = false
            },
            onError = { error ->
                runOnUiThread {
                    statusText.text = "Koneksi Bermasalah: $error"
                }
                isWsProcessing = false
            }
        )
        wsClient?.connect()
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            imageCapture = ImageCapture.Builder().build()

            imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setTargetResolution(android.util.Size(640, 480))
                .build()
                .also { analysis ->
                    analysis.setAnalyzer(cameraExecutor!!) { imageProxy ->
                        processFrameForRealtime(imageProxy)
                    }
                }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,
                    preview,
                    imageCapture,
                    imageAnalysis
                )
            } catch (e: Exception) {
                Log.e(TAG, "Camera binding error: ${e.message}")
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun processFrameForRealtime(imageProxy: ImageProxy) {
        if (isWsProcessing) {
            imageProxy.close()
            return
        }
        isWsProcessing = true

        CoroutineScope(Dispatchers.Default).launch {
            try {
                // Convert ImageProxy to Bitmap and then to Base64
                // We resize to 640 for faster transmission
                val bitmap = ImageUtils.imageProxyToBitmap(imageProxy)
                imageProxy.close()
                
                val resizedBitmap = Bitmap.createScaledBitmap(bitmap, 640, 640, true)
                val base64 = ImageUtils.bitmapToBase64(resizedBitmap)
                
                wsClient?.sendImage(base64)
                
                bitmap.recycle()
                resizedBitmap.recycle()
            } catch (e: Exception) {
                Log.e(TAG, "Error processing frame for WS", e)
                imageProxy.close()
                isWsProcessing = false
            }
        }
    }

    private fun takePhoto() {
        val imageCapture = imageCapture ?: return
        
        progressBar.visibility = ProgressBar.VISIBLE
        statusText.text = "Mengambil foto..."

        val name = SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(System.currentTimeMillis())
        val photoFile = File(externalCacheDir, "wayang_$name.jpg")
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Gagal mengambil foto: ${exc.message}", exc)
                    runOnUiThread {
                        progressBar.visibility = ProgressBar.GONE
                        statusText.text = "Gagal mengambil foto"
                    }
                }
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val savedUri = Uri.fromFile(photoFile)
                    runOnUiThread {
                        progressBar.visibility = ProgressBar.GONE
                        moveToResult(savedUri)
                    }
                }
            }
        )
    }

    private fun selectFromGallery() {
        val intent = Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        galleryLauncher.launch(intent)
    }

    private fun moveToResult(uri: Uri) {
        val intent = Intent(this, WayangResultActivity::class.java).apply {
            putExtra("image_uri", uri.toString())
        }
        startActivity(intent)
        finish()
    }

    private fun allPermissionsGranted() =
        ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor?.shutdown()
        wsClient?.close()
    }
}
