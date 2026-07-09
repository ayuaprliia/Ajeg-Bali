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

class JejahitanFragment : Fragment() {

    private lateinit var adapter: JejahitanAdapter
    private lateinit var dataList: List<JejahitanModel>

    // Menghubungkan layout fragment_jejahitan.xml
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_jejahitan, container, false)
    }

    // Logika dijalankan setelah tampilan selesai dimuat
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val namaJejahitan = listOf(
            "Ceniga", "Ceper", "Ituk-ituk", "Kulit Peras", "Lis Senjata",
            "Sampian Duras", "Sampian Gantung", "Sampian Kwangen", "Sampian Padma",
            "Sampian Penjor", "Sampian Penyeneng", "Sampian Peras", "Sampian Plaus",
            "Sampian Sesayut", "Sampian Soda", "Sampian Sri Keliki", "Taledan", "Tamas"
        )

        dataList = namaJejahitan.map { nama ->
            JejahitanModel(nama, getDrawableId(nama))
        }

        // Perhatikan penggunaan 'view.findViewById' di dalam Fragment
        val recyclerView = view.findViewById<RecyclerView>(R.id.rvJejahitan)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        adapter = JejahitanAdapter(dataList) { item ->
            // Menggunakan requireContext() bukan this
            val intent = Intent(requireContext(), DetailActivity::class.java)
            intent.putExtra("EXTRA_NAMA", item.nama)
            intent.putExtra("EXTRA_GAMBAR", item.gambarResId)
            startActivity(intent)
        }
        recyclerView.adapter = adapter

        val etSearch = view.findViewById<EditText>(R.id.etSearch)
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                filterJejahitan(s.toString())
            }
        })

        val btnScan = view.findViewById<FloatingActionButton>(R.id.btnScan)
        btnScan.setOnClickListener {
            val intent = Intent(requireContext(), ScanActivity::class.java)
            // Nanti kita tambahkan "Pesan Rahasia" di sini untuk membedakan model
            startActivity(intent)
        }
    }

    private fun filterJejahitan(text: String) {
        val filteredList = ArrayList<JejahitanModel>()
        for (item in dataList) {
            if (item.nama.lowercase().contains(text.lowercase())) {
                filteredList.add(item)
            }
        }
        adapter.filterList(filteredList)
    }

    private fun getDrawableId(nama: String): Int {
        val namaFile = nama.lowercase().replace(" ", "_").replace("-", "_")
        // requireContext() menggantikan this
        val resourceId = resources.getIdentifier(namaFile, "drawable", requireContext().packageName)
        return if (resourceId != 0) resourceId else R.drawable.ajeg_bali
    }
}