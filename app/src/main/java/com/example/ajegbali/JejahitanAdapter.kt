package com.example.ajegbali

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class JejahitanAdapter(
    private var listJejahitan: List<JejahitanModel>,
    private val onClick: (JejahitanModel) -> Unit // Fungsi klik
) : RecyclerView.Adapter<JejahitanAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvNama: TextView = view.findViewById(R.id.tvItemName)
        val ivGambar: ImageView = view.findViewById(R.id.ivItemImage)
        var currentJob: Job? = null
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_jejahitan, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = listJejahitan[position]
        holder.tvNama.text = item.nama
        
        // Cancel background image loading job if it's still running
        holder.currentJob?.cancel()
        
        // Clear previous image and set transparent placeholder
        holder.ivGambar.setImageResource(android.R.color.transparent)
        
        // Load image in background to prevent UI freezing
        holder.currentJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                // First decode with inJustDecodeBounds=true to check dimensions
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                BitmapFactory.decodeResource(holder.itemView.resources, item.gambarResId, options)
                
                // Calculate inSampleSize
                val reqWidth = 400
                val reqHeight = 400
                var sampleSize = 1
                
                if (options.outHeight > reqHeight || options.outWidth > reqWidth) {
                    val halfHeight: Int = options.outHeight / 2
                    val halfWidth: Int = options.outWidth / 2
                    while (halfHeight / sampleSize >= reqHeight && halfWidth / sampleSize >= reqWidth) {
                        sampleSize *= 2
                    }
                }
                
                // Decode bitmap with inSampleSize set
                val decodeOptions = BitmapFactory.Options().apply {
                    inSampleSize = sampleSize
                }
                val bitmap = BitmapFactory.decodeResource(holder.itemView.resources, item.gambarResId, decodeOptions)
                
                // Set bitmap on UI thread
                withContext(Dispatchers.Main) {
                    holder.ivGambar.setImageBitmap(bitmap)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // In case of error, just set directly or leave transparent
                withContext(Dispatchers.Main) {
                    holder.ivGambar.setImageResource(item.gambarResId)
                }
            }
        }

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