package com.example.ajegbali

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView

// Import Gson
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken

// Import YouTube Player
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView

// Import Coroutines
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Import PyTorch
import org.pytorch.IValue
import org.pytorch.LiteModuleLoader
import org.pytorch.Module
import org.pytorch.torchvision.TensorImageUtils

// Import Retrofit & OkHttp
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern
import kotlin.math.exp

// ==========================================
// 1. STRUKTUR DATA GROQ API (Disatukan di sini)
// ==========================================
data class GroqRequest(
    @SerializedName("model") val model: String,
    @SerializedName("messages") val messages: List<GroqMessage>,
    @SerializedName("temperature") val temperature: Float = 0.3f
)

data class GroqMessage(
    @SerializedName("role") val role: String,
    @SerializedName("content") val content: String
)

data class GroqResponse(
    @SerializedName("choices") val choices: List<GroqChoice>?
)

data class GroqChoice(
    @SerializedName("message") val message: GroqMessage?
)

interface GroqApiService {
    @POST("openai/v1/chat/completions")
    suspend fun getChatCompletion(
        @Header("Authorization") authorization: String,
        @Body request: GroqRequest
    ): GroqResponse
}

// ==========================================
// 2. STRUKTUR DATA JSON LOKAL
// ==========================================
data class JejahitanResultModel(
    val nama_jejahitan: String,
    val deskripsi: String,
    val link_youtube_pembuatan: String
)

// ==========================================
// 3. KELAS UTAMA ACTIVITY
// ==========================================
class ResultActivity : AppCompatActivity() {

    // UI Components
    private lateinit var imgResult: ImageView
    private lateinit var txtPrediction: TextView
    private lateinit var txtInferenceTime: TextView
    private lateinit var txtDescription: TextView
    private lateinit var btnBack: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var youtubePlayerView: YouTubePlayerView
    private lateinit var lblVideo: TextView
    private lateinit var cardYoutube: CardView

    // AI & Data Variables
    private var mModule: Module? = null
    private var jsonData: List<JejahitanResultModel> = emptyList()

    // --- VARIABEL GROQ RETROFIT ---
    private val groqApiKey = "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"
    private lateinit var groqApiService: GroqApiService

    // --- VARIABEL YOUTUBE ---
    private var currentYouTubePlayer: YouTubePlayer? = null
    private var pendingVideoId: String? = null

    // DAFTAR KELAS
    private val classNames = arrayOf(
        "Ceniga", "Ceper", "Ituk-ituk", "Kulit Peras", "Sampian Gantung", "Sampian Kwangen", "Sampian Padma",
        "Sampian Penyeneng", "Sampian Peras", "Sampian Plaus", "Sampian Sesayut",
        "Sampian Sri Keliki", "Taledan", "Tamas", "Sampian Penjor", "Lis Senjata", "Sampian Soda", "Sampian Duras"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_result)

        // Hubungkan Komponen UI
        imgResult = findViewById(R.id.imgResult)
        txtPrediction = findViewById(R.id.txtPrediction)
        txtInferenceTime = findViewById(R.id.txtInferenceTime)
        txtDescription = findViewById(R.id.txtDescription)
        btnBack = findViewById(R.id.btnBack)
        progressBar = findViewById(R.id.progressBar)
        youtubePlayerView = findViewById(R.id.youtubePlayerView)
        lblVideo = findViewById(R.id.lblVideo)
        cardYoutube = findViewById(R.id.cardYoutube)

        // Setup Retrofit untuk Groq API
        setupGroqApiClient()

        // Setup YouTube Player
        lifecycle.addObserver(youtubePlayerView)
        youtubePlayerView.addYouTubePlayerListener(object : AbstractYouTubePlayerListener() {
            override fun onReady(youTubePlayer: YouTubePlayer) {
                currentYouTubePlayer = youTubePlayer
                pendingVideoId?.let { videoId ->
                    youTubePlayer.cueVideo(videoId, 0f)
                }
            }
        })

        // Load Data JSON Lokal
        jsonData = loadDataFromJson()
        progressBar.visibility = View.VISIBLE
        txtPrediction.text = "Menganalisis..."
        txtInferenceTime.text = ""
        txtDescription.text = "Mohon tunggu sebentar..."
        hideVideoSection()

        // Jalankan PyTorch di Background
        Thread {
            loadModelAndRunInference()
        }.start()

        btnBack.setOnClickListener { finish() }
    }

    // Fungsi inisialisasi Retrofit secara langsung
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

    private fun loadModelAndRunInference() {
        try {
            if (mModule == null) {
                val modelPath = assetFilePath(this, "jejahitan_mobile.ptl")
                mModule = LiteModuleLoader.load(modelPath)
            }

            val imageUriString = intent.getStringExtra("image_uri")
            if (imageUriString != null) {
                val uri = Uri.parse(imageUriString)
                runOnUiThread { imgResult.setImageURI(uri) }

                val bitmap = uriToBitmap(uri)
                if (bitmap != null && mModule != null) {
                    runInference(bitmap)
                }
            }
        } catch (e: Exception) {
            Log.e("AjegBali", "Error Inference", e)
            runOnUiThread {
                progressBar.visibility = View.GONE
                txtPrediction.text = "Error"
                txtDescription.text = "Gagal memuat model AI."
            }
        }
    }

    private fun runInference(bitmap: Bitmap) {
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, 256, 256, true)
        val inputTensor = TensorImageUtils.bitmapToFloat32Tensor(
            resizedBitmap,
            TensorImageUtils.TORCHVISION_NORM_MEAN_RGB,
            TensorImageUtils.TORCHVISION_NORM_STD_RGB
        )

        val startTime = System.nanoTime()
        val outputTensor = mModule!!.forward(IValue.from(inputTensor)).toTensor()
        val endTime = System.nanoTime()
        val inferenceTimeMs = (endTime - startTime) / 1_000_000

        val logits = outputTensor.dataAsFloatArray
        val probabilities = softmax(logits)

        var maxScore = -Float.MAX_VALUE
        var maxScoreIdx = -1
        for (i in probabilities.indices) {
            if (probabilities[i] > maxScore) {
                maxScore = probabilities[i]
                maxScoreIdx = i
            }
        }

        runOnUiThread {
            progressBar.visibility = View.GONE
            val THRESHOLD = 0.50f
            txtInferenceTime.text = "Waktu komputasi: ${inferenceTimeMs} ms"

            if (maxScoreIdx >= 0 && maxScoreIdx < classNames.size) {
                val confidencePercent = (maxScore * 100).toInt()

                if (maxScore >= THRESHOLD) {
                    val detectedClass = classNames[maxScoreIdx]
                    txtPrediction.text = "[$confidencePercent%] $detectedClass"
                    txtPrediction.setTextColor(Color.parseColor("#8F9E8B"))

                    val detailItem = jsonData.find {
                        it.nama_jejahitan.equals(detectedClass, ignoreCase = true)
                    }

                    if (detailItem != null) {
                        txtDescription.text = "Memperhalus deskripsi dengan AI..."
                        txtDescription.setTextColor(Color.GRAY)
                        prepareVideo(detailItem.link_youtube_pembuatan)

                        // Panggil Groq API menggunakan Retrofit
                        refineDescriptionWithGroq(detailItem)
                    } else {
                        txtDescription.text = "Deskripsi belum tersedia."
                        hideVideoSection()
                    }
                } else {
                    txtPrediction.text = "Objek Tidak Dikenali"
                    txtPrediction.setTextColor(Color.RED)
                    txtDescription.text = "AI kurang yakin ($confidencePercent%)."
                    hideVideoSection()
                }
            } else {
                txtPrediction.text = "Error Klasifikasi"
                hideVideoSection()
            }
        }
    }

    // Fungsi memanggil API Groq
    private fun refineDescriptionWithGroq(detailItem: JejahitanResultModel) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Buat prompt yang optimal untuk meringkas dan memperhalus
                // 1. Buat prompt yang jauh lebih tegas
                val systemPrompt = """
                    Kamu adalah ahli budaya Bali. Tugasmu meringkas deskripsi seni tradisional Bali menjadi 1 paragraf padat.
                    
                    ATURAN KETAT:
                    1. LANGSUNG tuliskan isi ringkasan. DILARANG KERAS menggunakan kalimat pengantar/basa-basi (seperti "Berikut adalah...", "Ini ringkasannya...", dll).
                    2. DILARANG KERAS menggunakan format Markdown (seperti bintang ganda ** untuk bold).
                    3. Jangan gunakan format daftar (bullet points) atau numbering.
                    4. Buat ringkasan yang padat dan mudah dipahami
                    5. Gunakan bahasa Indonesia yang sopan dan profesional
                    6. Pertahankan makna dan nilai budaya asli
                    7. Hindari penggunaan kata-kata yang terlalu teknis atau rumit
                    8. Tambahkan sentuhan yang menunjukkan kekayaan budaya Bali
                """.trimIndent()

                val userPrompt = """
                    Ringkas deskripsi Jejahitan "${detailItem.nama_jejahitan}" berikut:
                    ${detailItem.deskripsi}
                """.trimIndent()

                val request = GroqRequest(
                    model = "llama-3.1-8b-instant",
                    messages = listOf(
                        GroqMessage(role = "system", content = systemPrompt),
                        GroqMessage(role = "user", content = userPrompt)
                    )
                )

                val response = groqApiService.getChatCompletion(
                    authorization = "Bearer $groqApiKey",
                    request = request
                )

                // 2. Tambahkan pembersih teks ganda (menghilangkan sisa bintang jika AI masih bandel)
                val aiResponseText = response.choices?.firstOrNull()?.message?.content
                    ?.replace("**", "")
                    ?.replace("*", "")

                withContext(Dispatchers.Main) {
                    if (!aiResponseText.isNullOrBlank()) {
                        txtDescription.text = aiResponseText.trim()
                        txtDescription.setTextColor(Color.DKGRAY)
                    } else {
                        txtDescription.text = detailItem.deskripsi
                        txtDescription.setTextColor(Color.DKGRAY)
                    }
                }

            } catch (e: Exception) {
                Log.e("GroqAPI", "Gagal menghubungi API Groq: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    // Fallback ke teks asli jika gagal internet
                    txtDescription.text = detailItem.deskripsi
                    txtDescription.setTextColor(Color.DKGRAY)
                }
            }
        }
    }

    // --- LOGIKA VIDEO YOUTUBE ---
    private fun prepareVideo(url: String) {
        val videoId = getYoutubeVideoId(url)
        if (videoId != null) {
            lblVideo.visibility = View.VISIBLE
            cardYoutube.visibility = View.VISIBLE
            pendingVideoId = videoId
            currentYouTubePlayer?.cueVideo(videoId, 0f)
        } else {
            hideVideoSection()
        }
    }

    private fun hideVideoSection() {
        lblVideo.visibility = View.GONE
        cardYoutube.visibility = View.GONE
    }

    private fun getYoutubeVideoId(url: String): String? {
        val pattern = "(?<=watch\\?v=|/videos/|embed\\/|youtu.be\\/|\\/v\\/|\\/e\\/|watch\\?v%3D|watch\\?feature=player_embedded&v=|%2Fvideos%2F|embed%\u200C\u200B2F|youtu.be%2F|%2Fv%2F)[^#\\&\\?\\n]*"
        val compiledPattern = Pattern.compile(pattern)
        val matcher = compiledPattern.matcher(url)
        return if (matcher.find()) matcher.group() else null
    }

    // --- UTILS ---
    private fun loadDataFromJson(): List<JejahitanResultModel> {
        return try {
            val jsonString = assets.open("data_jejahitan.json").bufferedReader().use { it.readText() }
            val listType = object : TypeToken<List<JejahitanResultModel>>() {}.type
            Gson().fromJson(jsonString, listType)
        } catch (e: Exception) { emptyList() }
    }

    private fun softmax(logits: FloatArray): FloatArray {
        val expScores = FloatArray(logits.size)
        var sumExp = 0.0f
        val maxLogit = logits.maxOrNull() ?: 0.0f
        for (i in logits.indices) {
            expScores[i] = exp(logits[i] - maxLogit)
            sumExp += expScores[i]
        }
        val probs = FloatArray(logits.size)
        for (i in logits.indices) {
            probs[i] = expScores[i] / sumExp
        }
        return probs
    }

    private fun uriToBitmap(uri: Uri): Bitmap? {
        return try {
            val inputStream = contentResolver.openInputStream(uri)
            BitmapFactory.decodeStream(inputStream)
        } catch (e: Exception) { null }
    }

    private fun assetFilePath(context: Context, assetName: String): String {
        val file = File(context.filesDir, assetName)
        if (file.exists() && file.length() > 0) return file.absolutePath
        try {
            context.assets.open(assetName).use { inputStream ->
                FileOutputStream(file).use { outputStream ->
                    val buffer = ByteArray(4 * 1024)
                    var read: Int
                    while (inputStream.read(buffer).also { read = it } != -1) {
                        outputStream.write(buffer, 0, read)
                    }
                    outputStream.flush()
                }
                return file.absolutePath
            }
        } catch (e: Exception) { throw RuntimeException("Error copy asset", e) }
    }
}