package com.example.ajegbali

import android.graphics.Color
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.appbar.AppBarLayout
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import java.util.regex.Pattern
import kotlin.math.abs

class DetailActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail)

        // 1. HUBUNGKAN KOMPONEN
        val ivGambar = findViewById<ImageView>(R.id.ivDetailImage)
        val tvJudul = findViewById<TextView>(R.id.tvDetailName)
        val tvDeskripsi = findViewById<TextView>(R.id.tvDetailDesc)
        val youtubePlayerView = findViewById<YouTubePlayerView>(R.id.youtubePlayerView)

        val appBar = findViewById<AppBarLayout>(R.id.appBar)
        val tvToolbarTitle = findViewById<TextView>(R.id.tvToolbarTitle)

        // Setup Toolbar
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        toolbar.setNavigationOnClickListener { finish() }

        // --- LOGIKA GANTI WARNA TOOLBAR (BUNGLON) TETAP AMAN ---
        appBar.addOnOffsetChangedListener { appBarLayout, verticalOffset ->
            val scrollRange = appBarLayout.totalScrollRange
            val percentage = abs(verticalOffset).toFloat() / scrollRange.toFloat()

            if (percentage > 0.7f) {
                val warnaHijau = ContextCompat.getColor(this, R.color.ajeg_secondary)
                tvToolbarTitle.setTextColor(warnaHijau)
                toolbar.navigationIcon?.setTint(warnaHijau)
            } else {
                tvToolbarTitle.setTextColor(Color.WHITE)
                toolbar.navigationIcon?.setTint(Color.WHITE)
            }
        }

        // 2. TERIMA DATA (Tambahan EXTRA_KATEGORI)
        val namaItem = intent.getStringExtra("EXTRA_NAMA") ?: ""
        val gambarResId = intent.getIntExtra("EXTRA_GAMBAR", 0)
        val kategori = intent.getStringExtra("EXTRA_KATEGORI") ?: "JEJAHITAN" // Default Jejahitan

        tvJudul.text = namaItem
        ivGambar.setImageResource(gambarResId)

        // 3. PENTING: Lifecycle Observer
        lifecycle.addObserver(youtubePlayerView)

        // 4. LOAD DATA JSON SECARA DINAMIS
        var deskripsiText = "Deskripsi belum tersedia."
        var linkYoutube: String? = null

        if (kategori == "KETUPAT") {
            // Logika Pembacaan JSON Ketupat
            val dataKetupat = loadKetupatData()
            // Kita hapus kata "Ketupat " hanya saat mencari di dalam JSON
            val jsonKey = namaItem.replace("Ketupat ", "")
            val itemDitemukan = dataKetupat[jsonKey] // Langsung cari berdasarkan nama Key

            if (itemDitemukan != null) {
                deskripsiText = itemDitemukan.deskripsi
                linkYoutube = itemDitemukan.youtube
            }
        } else {
            // Logika Pembacaan JSON Jejahitan (Kode Asli Anda)
            val dataJejahitan = loadJejahitanData()
            val itemDitemukan = dataJejahitan.find { it.nama_jejahitan == namaItem }

            if (itemDitemukan != null) {
                deskripsiText = itemDitemukan.deskripsi
                linkYoutube = itemDitemukan.link_youtube_pembuatan
            }
        }

        // 5. TAMPILKAN DATA KE UI
        tvDeskripsi.text = deskripsiText

        // --- SETUP YOUTUBE PLAYER JIKA LINK TERSEDIA ---
        if (!linkYoutube.isNullOrEmpty()) {
            val videoId = getYoutubeVideoId(linkYoutube)
            if (videoId != null) {
                youtubePlayerView.addYouTubePlayerListener(object : AbstractYouTubePlayerListener() {
                    override fun onReady(youTubePlayer: YouTubePlayer) {
                        youTubePlayer.cueVideo(videoId, 0f)
                    }
                })
            }
        }
    }

    // Fungsi Ambil ID Video YouTube
    private fun getYoutubeVideoId(url: String): String? {
        val pattern = "(?<=watch\\?v=|/videos/|embed\\/|youtu.be\\/|\\/v\\/|\\/e\\/|watch\\?v%3D|watch\\?feature=player_embedded&v=|%2Fvideos%2F|embed%\u200C\u200B2F|youtu.be%2F|%2Fv%2F)[^#\\&\\?\\n]*"
        val compiledPattern = Pattern.compile(pattern)
        val matcher = compiledPattern.matcher(url)
        return if (matcher.find()) matcher.group() else null
    }

    // Fungsi Baca JSON Jejahitan (Kode Asli)
    private fun loadJejahitanData(): List<JejahitanJsonModel> {
        return try {
            val jsonString = assets.open("data_jejahitan.json").bufferedReader().use { it.readText() }
            val listType = object : TypeToken<List<JejahitanJsonModel>>() {}.type
            Gson().fromJson(jsonString, listType)
        } catch (e: Exception) {
            emptyList()
        }
    }

    // Fungsi Baru: Baca JSON Ketupat
    private fun loadKetupatData(): Map<String, KetupatJsonModel> {
        return try {
            val jsonString = assets.open("data_ketupat.json").bufferedReader().use { it.readText() }
            val mapType = object : TypeToken<Map<String, KetupatJsonModel>>() {}.type
            Gson().fromJson(jsonString, mapType)
        } catch (e: Exception) {
            emptyMap()
        }
    }
}

// Model Data Jejahitan (Kode Asli)
data class JejahitanJsonModel(
    val nama_jejahitan: String,
    val deskripsi: String,
    val link_youtube_pembuatan: String
)

// Model Data Ketupat (Baru)
data class KetupatJsonModel(
    val deskripsi: String,
    val youtube: String
)