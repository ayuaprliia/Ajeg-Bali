package com.example.ajegbali.feature.wayang

import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.example.ajegbali.R
import com.example.ajegbali.data.remote.repository.WayangRepository

class WayangDetailActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wayang_detail)

        // Hubungkan ke View di XML
        val ivWayangDetail = findViewById<ImageView>(R.id.ivWayangDetail)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        val tvWayangName = findViewById<TextView>(R.id.tvWayangName)
        val tvCategoryGroup = findViewById<TextView>(R.id.tvCategoryGroup)
        val tvAliases = findViewById<TextView>(R.id.tvAliases)
        val tvDescription = findViewById<TextView>(R.id.tvDescription)
        val tvPhilosophy = findViewById<TextView>(R.id.tvPhilosophy)
        val tvVisualTraits = findViewById<TextView>(R.id.tvVisualTraits)

        // Aksi Tombol Kembali
        toolbar.setNavigationOnClickListener {
            finish()
        }

        // Ambil ID wayang dari kiriman intent saat diklik dari halaman sebelumnya
        val wayangId = intent.getStringExtra("EXTRA_WAYANG_ID") ?: return

        // Tarik semua data dari Repository teman Anda berdasarkan ID tersebut
        val wayangData = WayangRepository.getById(wayangId)

        if (wayangData != null) {
            ivWayangDetail.setImageResource(wayangData.imageResId)
            tvWayangName.text = "Wayang ${wayangData.name}"
            tvCategoryGroup.text = "Kategori: ${wayangData.category.name} • ${wayangData.group}"
            tvAliases.text = "Alias: ${wayangData.aliases.joinToString(", ")}"
            tvDescription.text = wayangData.description
            tvPhilosophy.text = wayangData.philosophy

            // Mengubah daftar ciri visual jadi ada titik-titiknya (bullet points)
            val bulletPoints = wayangData.visualTraits.joinToString("\n") { "• $it" }
            tvVisualTraits.text = bulletPoints
        }
    }
}
