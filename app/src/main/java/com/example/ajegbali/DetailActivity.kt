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

        // Komponen untuk efek warna (Pastikan ID tvToolbarTitle ada di XML)
        val appBar = findViewById<AppBarLayout>(R.id.appBar)
        val tvToolbarTitle = findViewById<TextView>(R.id.tvToolbarTitle)

        // Setup Toolbar
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        toolbar.setNavigationOnClickListener { finish() }

        // --- LOGIKA GANTI WARNA TOOLBAR (BUNGLON) ---
        appBar.addOnOffsetChangedListener { appBarLayout, verticalOffset ->
            val scrollRange = appBarLayout.totalScrollRange
            val percentage = abs(verticalOffset).toFloat() / scrollRange.toFloat()

            // Jika scroll sudah lebih dari 70% (Latar Putih muncul)
            if (percentage > 0.7f) {
                // Ubah Warna jadi HIJAU (ajeg_secondary)
                val warnaHijau = ContextCompat.getColor(this, R.color.ajeg_secondary)
                tvToolbarTitle.setTextColor(warnaHijau)
                toolbar.navigationIcon?.setTint(warnaHijau)
            } else {
                // Jika masih di atas (Latar Gambar)
                // Ubah Warna jadi PUTIH
                tvToolbarTitle.setTextColor(Color.WHITE)
                toolbar.navigationIcon?.setTint(Color.WHITE)
            }
        }

        // 2. TERIMA DATA
        val namaJejahitan = intent.getStringExtra("EXTRA_NAMA")
        val gambarResId = intent.getIntExtra("EXTRA_GAMBAR", 0)

        tvJudul.text = namaJejahitan
        ivGambar.setImageResource(gambarResId)

        // 3. PENTING: Lifecycle Observer
        lifecycle.addObserver(youtubePlayerView)

        // 4. LOAD DATA JSON
        val dataJson = loadDataFromJson()
        val itemDitemukan = dataJson.find { it.nama_jejahitan == namaJejahitan }

        if (itemDitemukan != null) {
            tvDeskripsi.text = itemDitemukan.deskripsi

            val linkYoutube = itemDitemukan.link_youtube_pembuatan
            val videoId = getYoutubeVideoId(linkYoutube)

            if (videoId != null) {
                // --- SETUP YOUTUBE PLAYER ---
                youtubePlayerView.addYouTubePlayerListener(object : AbstractYouTubePlayerListener() {
                    override fun onReady(youTubePlayer: YouTubePlayer) {
                        youTubePlayer.cueVideo(videoId, 0f)
                    }
                })
            }
        } else {
            tvDeskripsi.text = "Deskripsi belum tersedia."
        }
    }

    // Fungsi Ambil ID Video
    private fun getYoutubeVideoId(url: String): String? {
        val pattern = "(?<=watch\\?v=|/videos/|embed\\/|youtu.be\\/|\\/v\\/|\\/e\\/|watch\\?v%3D|watch\\?feature=player_embedded&v=|%2Fvideos%2F|embed%\u200C\u200B2F|youtu.be%2F|%2Fv%2F)[^#\\&\\?\\n]*"
        val compiledPattern = Pattern.compile(pattern)
        val matcher = compiledPattern.matcher(url)
        return if (matcher.find()) matcher.group() else null
    }

    // Fungsi Baca JSON
    private fun loadDataFromJson(): List<JejahitanJsonModel> {
        val jsonString = assets.open("data_jejahitan.json").bufferedReader().use { it.readText() }
        val listType = object : TypeToken<List<JejahitanJsonModel>>() {}.type
        return Gson().fromJson(jsonString, listType)
    }
}

data class JejahitanJsonModel(
    val nama_jejahitan: String,
    val deskripsi: String,
    val link_youtube_pembuatan: String
)