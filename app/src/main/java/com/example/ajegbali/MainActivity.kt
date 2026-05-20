package com.example.ajegbali

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MainActivity : AppCompatActivity() {

    // 1. DEKLARASI VARIABEL DI TINGKAT KELAS
    private lateinit var adapter: JejahitanAdapter
    private lateinit var dataList: List<JejahitanModel>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 2. DATA JEJAHITAN
        val namaJejahitan = listOf(
            "Ceniga", "Ceper", "Ituk-ituk", "Kulit Peras", "Lis Senjata",
            "Sampian Duras", "Sampian Gantung", "Sampian Kwangen", "Sampian Padma",
            "Sampian Penjor", "Sampian Penyeneng", "Sampian Peras", "Sampian Plaus",
            "Sampian Sesayut", "Sampian Soda", "Sampian Sri Keliki", "Taledan", "Tamas"
        )

        // Masukkan data ke dalam variabel kelas 'dataList'
        dataList = namaJejahitan.map { nama ->
            JejahitanModel(nama, getDrawableId(nama))
        }

        // 3. SETUP RECYCLERVIEW & ADAPTER
        val recyclerView = findViewById<RecyclerView>(R.id.rvJejahitan)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Inisialisasi adapter dan pasang ke RecyclerView
        adapter = JejahitanAdapter(dataList) { item ->
            // SAAT ITEM DIKLIK: Pindah ke DetailActivity
            val intent = Intent(this, DetailActivity::class.java)
            intent.putExtra("EXTRA_NAMA", item.nama)
            intent.putExtra("EXTRA_GAMBAR", item.gambarResId)
            startActivity(intent)
        }
        recyclerView.adapter = adapter

        // 4. SETUP SEARCH BAR (KOLOM PENCARIAN)
        val etSearch = findViewById<EditText>(R.id.etSearch)
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                // Panggil fungsi filter setiap kali ada ketikan
                filterJejahitan(s.toString())
            }
        })

        // 5. TOMBOL SCAN KELUAR/KAMERA
        val btnScan = findViewById<FloatingActionButton>(R.id.btnScan)
        btnScan.setOnClickListener {
            val intent = Intent(this, ScanActivity::class.java)
            startActivity(intent)
        }
    }

    // --- FUNGSI BARU: LOGIKA PENCARIAN ---
    private fun filterJejahitan(text: String) {
        // Buat daftar kosong untuk menampung hasil pencarian sementara
        val filteredList = ArrayList<JejahitanModel>()

        // Cek satu per satu data dari 'dataList' utama
        for (item in dataList) {
            // Jika nama jejahitan mengandung huruf yang diketik (abaikan huruf besar/kecil)
            if (item.nama.lowercase().contains(text.lowercase())) {
                filteredList.add(item) // Masukkan ke daftar sementara
            }
        }

        // Kirim daftar sementara tersebut ke Adapter untuk ditampilkan di layar
        adapter.filterList(filteredList)
    }

    // --- LOGIKA MENCARI GAMBAR OTOMATIS ---
    private fun getDrawableId(nama: String): Int {
        val namaFile = nama.lowercase()
            .replace(" ", "_")
            .replace("-", "_")

        val resourceId = resources.getIdentifier(namaFile, "drawable", packageName)
        return if (resourceId != 0) resourceId else R.drawable.logo_splash
    }
}