package com.example.ajegbali.feature.wayang

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.ajegbali.R
import com.example.ajegbali.data.model.JejahitanModel
import com.example.ajegbali.data.remote.repository.WayangRepository
import com.example.ajegbali.feature.jejahitan.JejahitanAdapter
import com.google.android.material.floatingactionbutton.FloatingActionButton

class WayangFragment : Fragment() {

    // Menggunakan JejahitanAdapter karena struktur UI daftarnya sama
    private lateinit var adapter: JejahitanAdapter
    private lateinit var dataList: List<JejahitanModel>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_wayang, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Daftar label nama wayang yang Anda berikan
        val namaWayang = listOf(
            "Acintya", "Arjuna", "Bhatara Siwa", "Bima", "Delem",
            "Durga", "Duryodana", "Krisna", "Kunti", "Madri",
            "Merdah", "Nakula-Sadewa", "Sangut", "Sengkuni", "Tualen", "Yudhistira"
        )

        // Menambahkan kata "Wayang " di depan nama untuk UI
        // Memanggil getDrawableId untuk otomatis mencari gambar berdasarkan nama
        dataList = namaWayang.map { nama ->
            JejahitanModel("Wayang $nama", getDrawableId(nama))
        }

        val recyclerView = view.findViewById<RecyclerView>(R.id.rvWayang)
        // Menerapkan Grid dengan 2 kolom langsung dari Kotlin agar lebih terjamin
        recyclerView.layoutManager = GridLayoutManager(requireContext(), 2)

        adapter = JejahitanAdapter(dataList) { item ->
            // Mengarah ke halaman detail Wayang yang baru
            val intent = Intent(requireContext(), WayangDetailActivity::class.java)

            // Mencari ID asli dari tulisan UI (misal: "Wayang Arjuna" -> "Arjuna")
            val namaAsli = item.nama.replace("Wayang ", "")
            val idWayang = WayangRepository.search(namaAsli).firstOrNull()?.id

            // Kirim ID tersebut ke halaman detail
            intent.putExtra("EXTRA_WAYANG_ID", idWayang)
            startActivity(intent)
        }
        recyclerView.adapter = adapter

        val etSearch = view.findViewById<EditText>(R.id.etSearchWayang)
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

        val btnScan = view.findViewById<FloatingActionButton>(R.id.btnScanWayang)
        btnScan.setOnClickListener {
            val intent = Intent(requireContext(), WayangScanActivity::class.java)
            startActivity(intent)
        }
    }

    private fun getDrawableId(nama: String): Int {
        // Mengubah huruf menjadi kecil, serta mengganti spasi dan strip "-" menjadi underscore "_"
        // Contoh: "Nakula-Sadewa" menjadi "nakula_sadewa"
        val namaFormat = nama.lowercase().replace(" ", "_").replace("-", "_")

        // Membentuk format nama file sesuai yang ada di drawable (cth: wayang_arjuna)
        val namaFile = "wayang_$namaFormat"

        val resourceId = resources.getIdentifier(namaFile, "drawable", requireContext().packageName)
        // Jika gambar tidak ditemukan di drawable, akan pakai logo ajeg_bali sebagai fallback sementara
        return if (resourceId != 0) resourceId else R.drawable.ajeg_bali
    }
}
