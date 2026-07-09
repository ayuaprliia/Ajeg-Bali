package com.example.ajegbali

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton

class KetupatFragment : Fragment() {

    private lateinit var adapter: JejahitanAdapter
    private lateinit var dataList: List<JejahitanModel>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_ketupat, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val namaKetupat = listOf(
            "Bagia", "Dampulan", "Gatep", "Gong", "Kukur",
            "Lepet", "Lojor", "Nasi", "Sari", "Sidakarya",
            "Sidapurna", "Sirikan", "Taluh"
        )

        // MENGUBAH TAMPILAN NAMA DI SINI
        // Kita tambahkan kata "Ketupat " di depan namanya untuk ditampilkan di UI
        // Tapi getDrawableId tetap menggunakan variabel 'nama' asli (misal: "Bagia")
        dataList = namaKetupat.map { nama ->
            JejahitanModel("Ketupat $nama", getDrawableId(nama))
        }

        val recyclerView = view.findViewById<RecyclerView>(R.id.rvKetupat)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        adapter = JejahitanAdapter(dataList) { item ->
            val intent = Intent(requireContext(), DetailActivity::class.java)
            // item.nama sekarang berisi "Ketupat Bagia"
            intent.putExtra("EXTRA_NAMA", item.nama)
            intent.putExtra("EXTRA_GAMBAR", item.gambarResId)
            intent.putExtra("EXTRA_KATEGORI", "KETUPAT")
            startActivity(intent)
        }
        recyclerView.adapter = adapter

        val etSearch = view.findViewById<EditText>(R.id.etSearchKetupat)
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val text = s.toString()
                val filteredList = dataList.filter {
                    it.nama.lowercase().contains(text.lowercase())
                }
                adapter.filterList(ArrayList(filteredList))
            }
        })

        val btnScan = view.findViewById<FloatingActionButton>(R.id.btnScanKetupat)
        btnScan.setOnClickListener {
            val intent = Intent(requireContext(), ScanActivity::class.java)
            intent.putExtra("EXTRA_KATEGORI", "KETUPAT")
            startActivity(intent)
        }
    }

    private fun getDrawableId(nama: String): Int {
        val namaFormat = nama.lowercase().replace(" ", "_").replace("-", "_")
        val namaFile = "img_ketupat_$namaFormat"

        val resourceId = resources.getIdentifier(namaFile, "drawable", requireContext().packageName)
        return if (resourceId != 0) resourceId else R.drawable.ajeg_bali
    }
}