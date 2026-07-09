package com.example.ajegbali

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.View

class BoundingBoxOverlay(context: Context) : View(context) {
    var results: List<DetectionResult> = emptyList()
    var imageWidth: Int = 1
    var imageHeight: Int = 1

    private val boxPaint = Paint().apply {
        color = Color.parseColor("#FFD700") // Emas / Gold
        style = Paint.Style.STROKE
        strokeWidth = 8f
    }
    private val textBgPaint = Paint().apply {
        color = Color.parseColor("#B3000000") // Hitam transparan
        style = Paint.Style.FILL
    }
    private val textPaint = Paint().apply {
        color = Color.parseColor("#FFD700")
        textSize = 45f
        isAntiAlias = true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (results.isEmpty()) return

        // Menghitung skala agar kotak deteksi akurat dengan ukuran layar HP
        val scaleX = width.toFloat() / imageWidth
        val scaleY = height.toFloat() / imageHeight
        val scale = maxOf(scaleX, scaleY) // Menyesuaikan dengan FILL_CENTER kamera

        val offsetX = (width - imageWidth * scale) / 2f
        val offsetY = (height - imageHeight * scale) / 2f

        for (result in results) {
            val box = result.boundingBox

            // Konversi koordinat AI (0.0 - 1.0) menjadi koordinat pixel di layar
            val left = offsetX + (box.left * imageWidth * scale)
            val top = offsetY + (box.top * imageHeight * scale)
            val right = offsetX + (box.right * imageWidth * scale)
            val bottom = offsetY + (box.bottom * imageHeight * scale)

            // Menggambar Kotak
            val rect = RectF(left, top, right, bottom)
            canvas.drawRect(rect, boxPaint)

            // Menggambar Label Teks dan Persentase
            val text = "${result.characterName} ${(result.confidence * 100).toInt()}%"
            val textWidth = textPaint.measureText(text)
            canvas.drawRect(left, top - 65f, left + textWidth + 20f, top, textBgPaint)
            canvas.drawText(text, left + 10f, top - 15f, textPaint)
        }
    }
}