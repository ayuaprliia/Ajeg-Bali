package com.example.ajegbali

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class JejahitanAdapter(
    private var listJejahitan: List<JejahitanModel>,
    private val onClick: (JejahitanModel) -> Unit // Fungsi klik
) : RecyclerView.Adapter<JejahitanAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvNama: TextView = view.findViewById(R.id.tvItemName)
        val ivGambar: ImageView = view.findViewById(R.id.ivItemImage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_jejahitan, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = listJejahitan[position]
        holder.tvNama.text = item.nama
        // Nanti gambar bisa diganti logic-nya, sementara pakai placeholder
        holder.ivGambar.setImageResource(item.gambarResId)

        // Klik item
        holder.itemView.setOnClickListener {
            onClick(item)
        }
    }

    override fun getItemCount() = listJejahitan.size
    fun filterList(filteredList: List<JejahitanModel>) {
        // Ganti daftar lama dengan daftar baru hasil pencarian
        listJejahitan = filteredList
        // Beritahu layar untuk me-refresh/menggambar ulang tampilannya
        notifyDataSetChanged()
    }
}