package com.example.ajegbali.feature.wayang

import android.content.Intent
import android.graphics.*
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.example.ajegbali.BuildConfig
import com.example.ajegbali.R
import com.example.ajegbali.data.Result
import com.example.ajegbali.data.model.WayangCharacter
import com.example.ajegbali.data.remote.repository.PredictionRepository
import com.example.ajegbali.data.remote.repository.WayangRepository
import com.example.ajegbali.data.remote.retrofit.ApiConfig
import com.example.ajegbali.ml.wayang.WayangDetectionResult
import com.example.ajegbali.feature.ketupat.GroqMessage
import com.example.ajegbali.feature.ketupat.GroqRequest
import com.example.ajegbali.feature.ketupat.GroqApiService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import android.graphics.BitmapFactory
import java.io.File
import java.io.FileOutputStream

class WayangResultActivity : AppCompatActivity() {

    // UI Components
    private lateinit var imgResult: ImageView
    private lateinit var txtPrediction: TextView
    private lateinit var txtInferenceTime: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var btnBack: Button

    // Dynamic character info container
    private lateinit var containerCharacterInfo: LinearLayout
    private lateinit var txtNoDetection: TextView

    // Repository
    private lateinit var predictionRepository: PredictionRepository

    // Groq API
    private val groqApiKey = BuildConfig.GROQ_API_KEY
    private lateinit var groqApiService: GroqApiService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wayang_result)

        // Hubungkan UI
        imgResult = findViewById(R.id.imgResult)
        txtPrediction = findViewById(R.id.txtPrediction)
        txtInferenceTime = findViewById(R.id.txtInferenceTime)
        progressBar = findViewById(R.id.progressBar)
        btnBack = findViewById(R.id.btnBack)

        containerCharacterInfo = findViewById(R.id.containerCharacterInfo)
        txtNoDetection = findViewById(R.id.txtNoDetection)

        // Initialize Repository
        predictionRepository = PredictionRepository.getInstance(ApiConfig.getInstance())
        setupGroqApiClient()

        progressBar.visibility = View.VISIBLE
        txtPrediction.text = "Menganalisis..."
        txtInferenceTime.text = ""
        containerCharacterInfo.visibility = View.GONE
        txtNoDetection.visibility = View.GONE

        btnBack.setOnClickListener {
            val intent = Intent(this, WayangScanActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
        }

        // Ambil gambar dari intent dan jalankan deteksi
        val imageUriString = intent.getStringExtra("image_uri")
        if (imageUriString != null) {
            val uri = Uri.parse(imageUriString)
            val bitmap = uriToBitmap(uri)
            val file = getFileFromUri(uri)
            
            if (bitmap != null && file != null) {
                runDetection(bitmap, file)
            } else {
                progressBar.visibility = View.GONE
                txtPrediction.text = "Error"
                txtNoDetection.text = "Gagal membaca gambar."
                txtNoDetection.visibility = View.VISIBLE
            }
        }
    }

    private fun runDetection(originalBitmap: Bitmap, file: File) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val startTime = System.currentTimeMillis()
                val result = predictionRepository.detectWayang(file)
                val endTime = System.currentTimeMillis()
                val inferenceTimeMs = endTime - startTime

                withContext(Dispatchers.Main) {
                    if (isFinishing || isDestroyed) return@withContext
                    
                    progressBar.visibility = View.GONE
                    txtInferenceTime.text = "Waktu komputasi: $inferenceTimeMs ms"

                    when (result) {
                        is Result.Success -> {
                            val response = result.data
                            val results = mutableListOf<WayangDetectionResult>()
                            
                            Log.d("WayangResult", "Response Success: boxes=${response.boxes?.size}, detections=${response.detections?.size}")

                            // Format 1: Raw YOLOv8 fields
                            response.boxes?.forEachIndexed { index, box ->
                                if (box.size >= 4) {
                                    val mappedBox = mapToNormalizedRect(box)
                                    Log.d("WayangResult", "Mapped raw box: $mappedBox")
                                    results.add(
                                        WayangDetectionResult(
                                            classIdx = response.classIds?.getOrNull(index) ?: 0,
                                            characterName = response.classNames?.getOrNull(index) ?: "Unknown",
                                            confidence = response.scores?.getOrNull(index) ?: 0f,
                                            boundingBox = mappedBox
                                        )
                                    )
                                }
                            }

                            // Format 2: Structured detections field
                            response.detections?.forEach { det ->
                                val box = det.box
                                if (box != null && box.size >= 4) {
                                    val mappedBox = mapToNormalizedRect(box)
                                    Log.d("WayangResult", "Mapped detection box: $mappedBox")
                                    results.add(
                                        WayangDetectionResult(
                                            classIdx = det.classId ?: 0,
                                            characterName = det.className ?: "Unknown",
                                            confidence = det.confidence ?: 0f,
                                            boundingBox = mappedBox
                                        )
                                    )
                                }
                            }
                            
                            Log.d("WayangResult", "Total detections parsed: ${results.size}")

                            // Gambar bounding box di atas bitmap
                            val annotatedBitmap = drawBoundingBoxes(originalBitmap, results)
                            imgResult.setImageBitmap(annotatedBitmap)

                            if (results.isNotEmpty()) {
                                val sortedResults = results.sortedByDescending { it.confidence }
                                if (sortedResults.size == 1) {
                                    val r = sortedResults[0]
                                    val confidencePercent = (r.confidence * 100).toInt()
                                    txtPrediction.text = "[$confidencePercent%] Wayang ${r.characterName}"
                                } else {
                                    val names = sortedResults.joinToString(", ") { it.characterName }
                                    txtPrediction.text = "${sortedResults.size} karakter: $names"
                                }
                                txtPrediction.setTextColor(Color.parseColor("#8F9E8B"))
                                showAllCharacterInfo(sortedResults)
                            } else {
                                txtPrediction.text = "Objek Tidak Dikenali"
                                txtPrediction.setTextColor(Color.RED)
                                containerCharacterInfo.visibility = View.GONE
                                txtNoDetection.visibility = View.VISIBLE
                            }
                        }
                        is Result.Error -> {
                            txtPrediction.text = "Error Deteksi"
                            txtPrediction.setTextColor(Color.RED)
                            txtNoDetection.text = result.error
                            txtNoDetection.visibility = View.VISIBLE
                        }
                        else -> {}
                    }
                }
            } catch (e: Exception) {
                Log.e("WayangResult", "Error saat deteksi", e)
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    txtPrediction.text = "Error Deteksi"
                    txtPrediction.setTextColor(Color.RED)
                    txtNoDetection.text = "Terjadi kesalahan saat memproses gambar."
                    txtNoDetection.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun drawBoundingBoxes(original: Bitmap, results: List<WayangDetectionResult>): Bitmap {
        val mutableBitmap = original.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(mutableBitmap)

        val boxPaint = Paint().apply {
            color = Color.parseColor("#FFD700")
            style = Paint.Style.STROKE
            strokeWidth = maxOf(mutableBitmap.width, mutableBitmap.height) * 0.006f
        }

        val textBgPaint = Paint().apply {
            color = Color.parseColor("#B3000000")
            style = Paint.Style.FILL
        }

        val textPaint = Paint().apply {
            color = Color.parseColor("#FFD700")
            textSize = maxOf(mutableBitmap.width, mutableBitmap.height) * 0.03f
            isAntiAlias = true
            typeface = Typeface.DEFAULT_BOLD
        }

        for (result in results) {
            val box = result.boundingBox
            val left = box.left * mutableBitmap.width
            val top = box.top * mutableBitmap.height
            val right = box.right * mutableBitmap.width
            val bottom = box.bottom * mutableBitmap.height

            val rect = RectF(left, top, right, bottom)
            canvas.drawRect(rect, boxPaint)

            val text = "${result.characterName} ${(result.confidence * 100).toInt()}%"
            val textWidth = textPaint.measureText(text)
            val textHeight = textPaint.textSize
            canvas.drawRect(left, top - textHeight - 10f, left + textWidth + 20f, top, textBgPaint)
            canvas.drawText(text, left + 10f, top - 8f, textPaint)
        }

        return mutableBitmap
    }

    private fun showAllCharacterInfo(results: List<WayangDetectionResult>) {
        containerCharacterInfo.removeAllViews()
        containerCharacterInfo.visibility = View.VISIBLE
        txtNoDetection.visibility = View.GONE

        for ((index, result) in results.withIndex()) {
            val wayangData = WayangRepository.search(result.characterName).firstOrNull()
            if (wayangData != null) {
                val card = createCharacterCard(index, result, wayangData)
                containerCharacterInfo.addView(card)
            }
        }

        if (containerCharacterInfo.childCount == 0) {
            containerCharacterInfo.visibility = View.GONE
            txtNoDetection.text = "Data karakter tidak ditemukan."
            txtNoDetection.visibility = View.VISIBLE
        }
    }

    private fun createCharacterCard(index: Int, result: WayangDetectionResult, wayangData: WayangCharacter): CardView {
        val cardView = CardView(this).apply {
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.bottomMargin = dpToPx(16)
            layoutParams = params
            radius = dpToPx(16).toFloat()
            cardElevation = dpToPx(4).toFloat()
        }

        val innerLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val pad = dpToPx(20)
            setPadding(pad, pad, pad, pad)
        }

        val confidencePercent = (result.confidence * 100).toInt()

        val tvName = TextView(this).apply {
            text = "Wayang ${wayangData.name}"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
            setTextColor(Color.parseColor("#8F9E8B"))
            setTypeface(typeface, Typeface.BOLD)
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.bottomMargin = dpToPx(4)
            layoutParams = params
        }

        val tvCategory = TextView(this).apply {
            text = "${wayangData.category} • ${wayangData.group} • [$confidencePercent%]"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setTextColor(Color.parseColor("#888888"))
        }

        val divider = View(this).apply {
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dpToPx(1)
            )
            params.topMargin = dpToPx(12)
            params.bottomMargin = dpToPx(12)
            layoutParams = params
            setBackgroundColor(Color.parseColor("#E0E0E0"))
        }

        val tvDescLabel = TextView(this).apply {
            text = "Deskripsi Umum"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            setTextColor(Color.parseColor("#8F9E8B"))
            setTypeface(typeface, Typeface.BOLD)
        }

        val tvDescription = TextView(this).apply {
            text = "Memperhalus deskripsi dengan AI..."
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setTextColor(Color.GRAY)
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.topMargin = dpToPx(8)
            layoutParams = params
            setLineSpacing(dpToPx(4).toFloat(), 1f)
        }

        val tvFilosofiLabel = TextView(this).apply {
            text = "Makna Filosofis"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            setTextColor(Color.parseColor("#8F9E8B"))
            setTypeface(typeface, Typeface.BOLD)
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.topMargin = dpToPx(16)
            layoutParams = params
        }

        val tvFilosofi = TextView(this).apply {
            text = wayangData.philosophy
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setTextColor(Color.GRAY)
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.topMargin = dpToPx(8)
            layoutParams = params
            setLineSpacing(dpToPx(4).toFloat(), 1f)
        }

        val tvKarakteristikLabel = TextView(this).apply {
            text = "Karakteristik & Ciri Visual"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            setTextColor(Color.parseColor("#8F9E8B"))
            setTypeface(typeface, Typeface.BOLD)
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.topMargin = dpToPx(16)
            layoutParams = params
        }

        val tvKarakteristik = TextView(this).apply {
            val visualStr = wayangData.visualTraits.joinToString(" • ")
            val sifatStr = wayangData.traits.joinToString(", ")
            text = "Sifat: $sifatStr\nVisual: $visualStr"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setTextColor(Color.GRAY)
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.topMargin = dpToPx(8)
            layoutParams = params
            setLineSpacing(dpToPx(4).toFloat(), 1f)
        }

        innerLayout.addView(tvName)
        innerLayout.addView(tvCategory)
        innerLayout.addView(divider)
        innerLayout.addView(tvDescLabel)
        innerLayout.addView(tvDescription)
        innerLayout.addView(tvFilosofiLabel)
        innerLayout.addView(tvFilosofi)
        innerLayout.addView(tvKarakteristikLabel)
        innerLayout.addView(tvKarakteristik)
        cardView.addView(innerLayout)

        refineDescriptionWithGroq(wayangData.name, wayangData.description, tvDescription)

        return cardView
    }

    private fun refineDescriptionWithGroq(namaWayang: String, deskripsiAsli: String, targetTextView: TextView) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val systemPrompt = """
                    Kamu adalah ahli budaya Bali. Tugasmu meringkas deskripsi Wayang Kulit Bali menjadi 1 paragraf padat.
                    ATURAN KETAT:
                    1. LANGSUNG tuliskan isi ringkasan. DILARANG KERAS basa-basi.
                    2. DILARANG KERAS menggunakan format Markdown (seperti bintang ganda ** untuk bold).
                    3. Jangan gunakan format daftar (bullet points).
                    4. Buat ringkasan padat, informatif, dan mudah dipahami.
                    5. Gunakan bahasa Indonesia profesional namun hangat.
                """.trimIndent()

                val userPrompt = """
                    Ringkas deskripsi Wayang Kulit "$namaWayang" berikut:
                    $deskripsiAsli
                """.trimIndent()

                val request = GroqRequest(
                    model = "llama-3.1-8b-instant",
                    messages = listOf(
                        GroqMessage(role = "system", content = systemPrompt),
                        GroqMessage(role = "user", content = userPrompt)
                    )
                )

                val response = groqApiService.getChatCompletion("Bearer $groqApiKey", request)
                val aiResponseText = response.choices?.firstOrNull()?.message?.content
                    ?.replace("**", "")?.replace("*", "")

                if (!isFinishing && !isDestroyed) {
                    withContext(Dispatchers.Main) {
                        if (!aiResponseText.isNullOrBlank()) {
                            targetTextView.text = aiResponseText.trim()
                        } else {
                            targetTextView.text = deskripsiAsli
                        }
                        targetTextView.setTextColor(Color.DKGRAY)
                    }
                }
            } catch (e: Exception) {
                Log.e("GroqAPI", "Gagal menghubungi API Groq", e)
                if (!isFinishing && !isDestroyed) {
                    withContext(Dispatchers.Main) {
                        targetTextView.text = deskripsiAsli
                        targetTextView.setTextColor(Color.DKGRAY)
                    }
                }
            }
        }
    }

    private fun setupGroqApiClient() {
        val loggingInterceptor = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.groq.com/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        groqApiService = retrofit.create(GroqApiService::class.java)
    }

    private fun uriToBitmap(uri: Uri): Bitmap? {
        return try {
            val inputStream = contentResolver.openInputStream(uri)
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream?.close()

            val maxDim = maxOf(options.outWidth, options.outHeight)
            var sampleSize = 1
            while (maxDim / sampleSize > 4000) { sampleSize *= 2 }

            val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            val inputStream2 = contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream2, null, decodeOptions)
            inputStream2?.close()
            
            // Koreksi rotasi berdasarkan EXIF metadata
            if (bitmap != null) {
                val rotatedBitmap = correctExifRotation(uri, bitmap)
                if (rotatedBitmap !== bitmap) {
                    bitmap.recycle()
                }
                rotatedBitmap
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("WayangResult", "Error membaca bitmap", e)
            null
        }
    }

    private fun correctExifRotation(uri: Uri, bitmap: Bitmap): Bitmap {
        return try {
            val inputStream = contentResolver.openInputStream(uri) ?: return bitmap
            val exif = android.media.ExifInterface(inputStream)
            val orientation = exif.getAttributeInt(
                android.media.ExifInterface.TAG_ORIENTATION,
                android.media.ExifInterface.ORIENTATION_NORMAL
            )
            inputStream.close()

            val matrix = Matrix()
            when (orientation) {
                androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                androidx.exifinterface.media.ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1f, 1f)
                androidx.exifinterface.media.ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.preScale(1f, -1f)
                else -> return bitmap
            }

            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } catch (e: Exception) {
            Log.e("WayangResult", "Error koreksi EXIF rotation", e)
            bitmap
        }
    }

    private fun getFileFromUri(uri: Uri): File? {
        return try {
            val file = File(cacheDir, "temp_wayang_prediction.jpg")
            val inputStream = contentResolver.openInputStream(uri)
            val outputStream = FileOutputStream(file)
            inputStream?.copyTo(outputStream)
            inputStream?.close()
            outputStream.close()
            file
        } catch (e: Exception) {
            Log.e("WayangResult", "Error getting file from uri", e)
            null
        }
    }

    private fun dpToPx(dp: Int): Int {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp.toFloat(), resources.displayMetrics).toInt()
    }

    /**
     * Helper to map backend coordinates to normalized RectF (0.0 to 1.0).
     * Handles YOLOv8 [x_center, y_center, width, height] format.
     */
    private fun mapToNormalizedRect(box: List<Float>): RectF {
        if (box.size < 4) return RectF()

        val cx = box[0]
        val cy = box[1]
        val w = box[2]
        val h = box[3]

        val isAbsolute = box.any { it > 1.1f }
        val scale = if (isAbsolute) 640f else 1.0f

        val left = (cx - w / 2f) / scale
        val top = (cy - h / 2f) / scale
        val right = (cx + w / 2f) / scale
        val bottom = (cy + h / 2f) / scale

        return RectF(
            left.coerceIn(0f, 1f),
            top.coerceIn(0f, 1f),
            right.coerceIn(0f, 1f),
            bottom.coerceIn(0f, 1f)
        )
    }
}
