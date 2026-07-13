package com.example.ajegbali.feature.chatbot

import android.os.Bundle
import android.util.Log
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.ajegbali.R
import com.example.ajegbali.data.model.ChatMessage
import com.example.ajegbali.data.repository.WayangRepository
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Activity chatbot AI untuk menjawab pertanyaan seputar kebudayaan Bali.
 * Menggunakan Gemini API dengan knowledge base dari data lokal aplikasi.
 *
 * Chatbot HANYA menjawab pertanyaan terkait:
 * - Jejahitan (18 jenis)
 * - Ketupat (13 jenis)
 * - Wayang Kulit Bali (16 karakter)
 *
 * Pertanyaan di luar konteks akan ditolak dengan sopan.
 */
class ChatbotActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "ChatbotActivity"
        private const val GEMINI_MODEL = "gemini-2.5-flash"
        private const val GEMINI_API_KEY = "AQ.Ab8RN6K4yRjTiSwKg39d6WHRI8kM5IkpCYLSRx_uaPJ7oN9eGQ"
        private const val GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/"
    }

    // UI
    private lateinit var rvChatMessages: RecyclerView
    private lateinit var etChatInput: EditText
    private lateinit var fabSend: FloatingActionButton
    private lateinit var btnBack: ImageView

    // Data
    private val chatMessages = mutableListOf<ChatMessage>()
    private lateinit var chatAdapter: ChatAdapter
    private lateinit var geminiApiService: GeminiApiService

    // Conversation history untuk multi-turn
    private val conversationHistory = mutableListOf<GeminiContent>()

    // System instruction yang berisi knowledge base
    private lateinit var systemInstruction: GeminiContent

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chatbot)

        initViews()
        setupRecyclerView()
        setupGeminiClient()
        buildSystemInstruction()
        showWelcomeMessage()
        setupListeners()
    }

    private fun initViews() {
        rvChatMessages = findViewById(R.id.rvChatMessages)
        etChatInput = findViewById(R.id.etChatInput)
        fabSend = findViewById(R.id.fabSend)
        btnBack = findViewById(R.id.btnBack)
    }

    private fun setupRecyclerView() {
        chatAdapter = ChatAdapter(chatMessages)
        val layoutManager = LinearLayoutManager(this)
        layoutManager.stackFromEnd = true
        rvChatMessages.layoutManager = layoutManager
        rvChatMessages.adapter = chatAdapter
    }

    private fun setupGeminiClient() {
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(GEMINI_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        geminiApiService = retrofit.create(GeminiApiService::class.java)
    }

    /**
     * Membangun system instruction dengan seluruh knowledge base dari data lokal.
     * Ini memastikan chatbot memiliki pengetahuan lengkap tentang semua objek budaya
     * yang tersedia di aplikasi.
     */
    private fun buildSystemInstruction() {
        val jejahitanData = loadJejahitanKnowledge()
        val ketupatData = loadKetupatKnowledge()
        val wayangData = buildWayangKnowledge()

        val systemPrompt = """
Kamu adalah "Asisten Budaya Bali" — asisten cerdas dalam aplikasi Ajeg Bali Lens yang bertugas membantu pengguna memahami kebudayaan Bali.

PERAN DAN KEPRIBADIAN:
- Kamu adalah asisten yang sopan, ramah, dan penuh pengetahuan tentang budaya Bali
- Gunakan bahasa Indonesia yang santun namun hangat dan mudah dipahami
- Sapa pengguna dengan hormat. Boleh menggunakan sapaan Bali seperti "Semeton" (saudara) jika sesuai konteks
- Berikan jawaban yang informatif, edukatif, dan menarik
- Jika memungkinkan, berikan konteks filosofis dan spiritual dari setiap objek budaya

CAKUPAN PENGETAHUAN (HANYA BOLEH MENJAWAB SEPUTAR INI):
1. JEJAHITAN — Seni merangkai janur/daun kelapa untuk sarana upacara Hindu Bali
2. KETUPAT — Anyaman daun lontar/janur untuk sarana upacara keagamaan Hindu Bali  
3. WAYANG KULIT BALI — Tokoh-tokoh pewayangan Bali

ATURAN KETAT:
1. PRIORITASKAN menjawab menggunakan DATA LOKAL yang diberikan di bawah ini.
2. Jika pengguna menanyakan karakter wayang, jejahitan, atau ketupat yang TIDAK ADA dalam data lokal, kamu DIIZINKAN menggunakan pengetahuan pribadimu untuk memberikan informasi umum yang ramah dan edukatif. NAMUN, kamu WAJIB memberitahu pengguna di awal bahwa karakter/objek tersebut belum tersedia di dalam dataset/aplikasi AjegBali, lalu berikan penjelasan umumnya.
3. Jika pengguna bertanya di LUAR topik kebudayaan Bali (khususnya Wayang, Jejahitan, Ketupat), tolak dengan sopan. Contoh respon:
   "Mohon maaf, Semeton. Saya hanya dapat membantu menjawab pertanyaan seputar kebudayaan Bali seperti Jejahitan, Ketupat, dan Wayang Kulit. Silakan ajukan pertanyaan terkait topik tersebut ya! 🙏"
4. DILARANG menggunakan format Markdown (seperti ** untuk bold, # untuk heading, atau * untuk italic)
5. Jawab dalam format paragraf yang rapi dan mudah dibaca
6. Maksimal 3 paragraf per jawaban agar ringkas dan padat

DATA JEJAHITAN (18 jenis):
$jejahitanData

DATA KETUPAT (13 jenis):
$ketupatData

DATA WAYANG KULIT BALI (16 karakter):
$wayangData
        """.trimIndent()

        systemInstruction = GeminiContent(
            role = null,
            parts = listOf(GeminiPart(text = systemPrompt))
        )
    }

    private fun loadJejahitanKnowledge(): String {
        return try {
            val jsonString = assets.open("data_jejahitan.json").bufferedReader().use { it.readText() }
            val listType = object : TypeToken<List<Map<String, String>>>() {}.type
            val dataList: List<Map<String, String>> = Gson().fromJson(jsonString, listType)

            dataList.joinToString("\n\n") { item ->
                val nama = item["nama_jejahitan"] ?: ""
                val deskripsi = item["deskripsi"] ?: ""
                "• $nama: $deskripsi"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Gagal memuat data jejahitan", e)
            "(Data jejahitan tidak tersedia)"
        }
    }

    private fun loadKetupatKnowledge(): String {
        return try {
            val jsonString = assets.open("data_ketupat.json").bufferedReader().use { it.readText() }
            val mapType = object : TypeToken<Map<String, Map<String, String>>>() {}.type
            val dataMap: Map<String, Map<String, String>> = Gson().fromJson(jsonString, mapType)

            dataMap.entries.joinToString("\n\n") { (nama, detail) ->
                val deskripsi = detail["deskripsi"] ?: ""
                "• Ketupat $nama: $deskripsi"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Gagal memuat data ketupat", e)
            "(Data ketupat tidak tersedia)"
        }
    }

    private fun buildWayangKnowledge(): String {
        return try {
            val characters = WayangRepository.getAll()
            characters.joinToString("\n\n") { char ->
                """• ${char.name} (${char.aliases.joinToString(", ")}):
Kategori: ${char.category.name}, Kelompok: ${char.group}
Sifat: ${char.traits.joinToString(", ")}
Deskripsi: ${char.description}
Filosofi: ${char.philosophy}
Ciri Visual: ${char.visualTraits.joinToString(", ")}"""
            }
        } catch (e: Exception) {
            Log.e(TAG, "Gagal memuat data wayang", e)
            "(Data wayang tidak tersedia)"
        }
    }

    private fun showWelcomeMessage() {
        val welcomeText = "Om Swastyastu, Semeton! 🙏\n\n" +
                "Saya adalah Asisten Budaya Bali dari Ajeg Bali Lens. " +
                "Saya siap membantu Anda mempelajari dan memahami kebudayaan Bali, khususnya tentang:\n\n" +
                "🪷 Jejahitan — Seni merangkai janur\n" +
                "🎋 Ketupat — Anyaman tradisional Bali\n" +
                "🎭 Wayang Kulit Bali — Tokoh pewayangan\n\n" +
                "Silakan ajukan pertanyaan Anda!"

        addBotMessage(welcomeText)
    }

    private fun setupListeners() {
        btnBack.setOnClickListener { finish() }

        fabSend.setOnClickListener { sendMessage() }

        etChatInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendMessage()
                true
            } else {
                false
            }
        }
    }

    private fun sendMessage() {
        val userText = etChatInput.text.toString().trim()
        if (userText.isEmpty()) return

        // Tambahkan pesan user ke UI
        addUserMessage(userText)
        etChatInput.text.clear()

        // Tambahkan ke conversation history
        conversationHistory.add(
            GeminiContent(
                role = "user",
                parts = listOf(GeminiPart(text = userText))
            )
        )

        // Tampilkan indikator loading
        val loadingMessage = ChatMessage(
            text = "Sedang mengetik...",
            isUser = false
        )
        chatMessages.add(loadingMessage)
        chatAdapter.notifyItemInserted(chatMessages.size - 1)
        scrollToBottom()

        // Kirim ke Gemini API
        fabSend.isEnabled = false
        etChatInput.isEnabled = false

        lifecycleScope.launch {
            try {
                val response = callGeminiApi()

                // Hapus loading message
                chatMessages.removeAt(chatMessages.size - 1)
                chatAdapter.notifyItemRemoved(chatMessages.size)

                if (response != null) {
                    addBotMessage(response)

                    // Tambahkan respon bot ke conversation history
                    conversationHistory.add(
                        GeminiContent(
                            role = "model",
                            parts = listOf(GeminiPart(text = response))
                        )
                    )
                } else {
                    addBotMessage("Mohon maaf, Semeton. Saya mengalami gangguan teknis saat ini. Silakan coba lagi dalam beberapa saat. 🙏")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error calling Gemini API", e)

                // Hapus loading message
                if (chatMessages.isNotEmpty() && chatMessages.last().text == "Sedang mengetik...") {
                    chatMessages.removeAt(chatMessages.size - 1)
                    chatAdapter.notifyItemRemoved(chatMessages.size)
                }

                addBotMessage("Mohon maaf, Semeton. Terjadi kesalahan koneksi. Pastikan perangkat Anda terhubung ke internet dan coba lagi. 🙏")
            } finally {
                fabSend.isEnabled = true
                etChatInput.isEnabled = true
            }
        }
    }

    /**
     * Memanggil Gemini API dengan conversation history dan system instruction.
     * Mengembalikan teks respon yang sudah dibersihkan dari format markdown.
     */
    private suspend fun callGeminiApi(): String? {
        return withContext(Dispatchers.IO) {
            try {
                val request = GeminiRequest(
                    contents = conversationHistory.toList(),
                    systemInstruction = systemInstruction,
                    generationConfig = GeminiGenerationConfig(
                        temperature = 0.4f,
                        topP = 0.95f,
                        topK = 40,
                        maxOutputTokens = 1024
                    )
                )

                val response = geminiApiService.generateContent(
                    model = GEMINI_MODEL,
                    apiKey = GEMINI_API_KEY,
                    request = request
                )

                val rawText = response.candidates
                    ?.firstOrNull()
                    ?.content
                    ?.parts
                    ?.firstOrNull()
                    ?.text

                // Bersihkan format markdown yang mungkin lolos
                rawText?.replace("**", "")
                    ?.replace("*", "")
                    ?.replace("##", "")
                    ?.replace("#", "")
                    ?.trim()

            } catch (e: Exception) {
                Log.e(TAG, "Gemini API call failed", e)
                null
            }
        }
    }

    private fun addUserMessage(text: String) {
        chatMessages.add(ChatMessage(text = text, isUser = true))
        chatAdapter.notifyItemInserted(chatMessages.size - 1)
        scrollToBottom()
    }

    private fun addBotMessage(text: String) {
        chatMessages.add(ChatMessage(text = text, isUser = false))
        chatAdapter.notifyItemInserted(chatMessages.size - 1)
        scrollToBottom()
    }

    private fun scrollToBottom() {
        rvChatMessages.post {
            if (chatMessages.isNotEmpty()) {
                rvChatMessages.smoothScrollToPosition(chatMessages.size - 1)
            }
        }
    }
}
