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
import com.example.ajegbali.R
import com.example.ajegbali.data.model.WayangCharacter
import com.example.ajegbali.data.repository.WayangRepository
import com.example.ajegbali.ml.wayang.WayangDetectionResult
import com.example.ajegbali.ml.wayang.WayangDetector
import com.example.ajegbali.feature.ketupat.GroqMessage
import com.example.ajegbali.feature.ketupat.GroqRequest
import com.example.ajegbali.feature.ketupat.GroqApiService

// Import Groq API classes (shared from ResultActivity)
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import java.util.concurrent.TimeUnit
import android.graphics.BitmapFactory

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

    // Detector
    private var wayangDetector: WayangDetector? = null

    // Groq API
    private val groqApiKey = "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"
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

        // Setup
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

        // Inisialisasi detector
        try {
            wayangDetector = WayangDetector(this)
        } catch (e: Exception) {
            Log.e("WayangResult", "Gagal memuat model", e)
            progressBar.visibility = View.GONE
            txtPrediction.text = "Error"
            txtNoDetection.text = "Gagal memuat model AI."
            txtNoDetection.visibility = View.VISIBLE
            return
        }

        // Ambil gambar dari intent dan jalankan deteksi
        val imageUriString = intent.getStringExtra("image_uri")
        if (imageUriString != null) {
            val uri = Uri.parse(imageUriString)
            val bitmap = uriToBitmap(uri)
            if (bitmap != null) {
                runDetection(bitmap)
            } else {
                progressBar.visibility = View.GONE
                txtPrediction.text = "Error"
                txtNoDetection.text = "Gagal membaca gambar."
                txtNoDetection.visibility = View.VISIBLE
            }
        }
    }

    private fun runDetection(originalBitmap: Bitmap) {
        // Jalankan inferensi di background thread
        Thread {
            try {
                val startTime = System.nanoTime()
                val results = wayangDetector?.detect(originalBitmap) ?: emptyList()
                val endTime = System.nanoTime()
                val inferenceTimeMs = (endTime - startTime) / 1_000_000

                // Gambar bounding box di atas bitmap
                val annotatedBitmap = drawBoundingBoxes(originalBitmap, results)

                // Pastikan activity masih aktif sebelum update UI
                if (isFinishing || isDestroyed) return@Thread

                runOnUiThread {
                    // Double check di main thread
                    if (isFinishing || isDestroyed) return@runOnUiThread

                    progressBar.visibility = View.GONE
                    txtInferenceTime.text = "Waktu komputasi: ${inferenceTimeMs} ms"

                    // Tampilkan gambar dengan bounding box
                    imgResult.setImageBitmap(annotatedBitmap)

                    if (results.isNotEmpty()) {
                        // Tampilkan SEMUA hasil deteksi
                        val totalDetected = results.size
                        val sortedResults = results.sortedByDescending { it.confidence }

                        // Tampilkan summary di txtPrediction
                        if (totalDetected == 1) {
                            val r = sortedResults[0]
                            val confidencePercent = (r.confidence * 100).toInt()
                            txtPrediction.text = "[$confidencePercent%] Wayang ${r.characterName}"
                        } else {
                            val names = sortedResults.joinToString(", ") { it.characterName }
                            txtPrediction.text = "$totalDetected karakter: $names"
                        }
                        txtPrediction.setTextColor(Color.parseColor("#8F9E8B"))

                        // Tampilkan info SEMUA karakter
                        showAllCharacterInfo(sortedResults)
                    } else {
                        txtPrediction.text = "Objek Tidak Dikenali"
                        txtPrediction.setTextColor(Color.RED)
                        containerCharacterInfo.visibility = View.GONE
                        txtNoDetection.visibility = View.VISIBLE
                    }
                }
            } catch (e: Exception) {
                Log.e("WayangResult", "Error saat deteksi", e)
                if (isFinishing || isDestroyed) return@Thread
                runOnUiThread {
                    if (isFinishing || isDestroyed) return@runOnUiThread
                    progressBar.visibility = View.GONE
                    txtPrediction.text = "Error Deteksi"
                    txtPrediction.setTextColor(Color.RED)
                    txtNoDetection.text = "Terjadi kesalahan saat memproses gambar."
                    txtNoDetection.visibility = View.VISIBLE
                }
            }
        }.start()
    }

    private fun drawBoundingBoxes(original: Bitmap, results: List<WayangDetectionResult>): Bitmap {
        // Buat salinan bitmap yang bisa digambar
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

            // Gambar kotak
            val rect = RectF(left, top, right, bottom)
            canvas.drawRect(rect, boxPaint)

            // Gambar label
            val text = "${result.characterName} ${(result.confidence * 100).toInt()}%"
            val textWidth = textPaint.measureText(text)
            val textHeight = textPaint.textSize
            canvas.drawRect(left, top - textHeight - 10f, left + textWidth + 20f, top, textBgPaint)
            canvas.drawText(text, left + 10f, top - 8f, textPaint)
        }

        return mutableBitmap
    }

    /**
     * Menampilkan info untuk SEMUA karakter yang terdeteksi.
     * Membuat card secara programatis untuk setiap karakter.
     */
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

        // Jika tidak ada data ditemukan sama sekali
        if (containerCharacterInfo.childCount == 0) {
            containerCharacterInfo.visibility = View.GONE
            txtNoDetection.text = "Data karakter tidak ditemukan."
            txtNoDetection.visibility = View.VISIBLE
        }
    }

    /**
     * Membuat CardView secara programatis untuk satu karakter wayang.
     */
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

        // Nomor urut + Confidence
        val confidencePercent = (result.confidence * 100).toInt()

        // Nama Karakter
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

        // Kategori & Group + Confidence
        val tvCategory = TextView(this).apply {
            text = "${wayangData.category} • ${wayangData.group} • [$confidencePercent%]"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setTextColor(Color.parseColor("#888888"))
        }

        // Divider
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

        // Label Deskripsi
        val tvDescLabel = TextView(this).apply {
            text = "Deskripsi Umum"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            setTextColor(Color.parseColor("#8F9E8B"))
            setTypeface(typeface, Typeface.BOLD)
        }

        // Deskripsi (placeholder, akan diisi oleh AI)
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

        // Label Filosofi
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

        // Label Karakteristik / Ciri Visual
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

        // Susun layout
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

        // Panggil Groq API untuk deskripsi
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
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
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
            val options = BitmapFactory.Options().apply {
                // Pertama, cek ukuran gambar
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream?.close()

            // Hitung sample size jika gambar terlalu besar (>4000px)
            val maxDim = maxOf(options.outWidth, options.outHeight)
            var sampleSize = 1
            while (maxDim / sampleSize > 4000) {
                sampleSize *= 2
            }

            // Decode dengan sample size yang tepat
            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
            }
            val inputStream2 = contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream2, null, decodeOptions)
            inputStream2?.close()
            bitmap
        } catch (e: Exception) {
            Log.e("WayangResult", "Error membaca bitmap", e)
            null
        }
    }

    private fun dpToPx(dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            resources.displayMetrics
        ).toInt()
    }

    override fun onDestroy() {
        super.onDestroy()
        wayangDetector?.close()
    }
}
