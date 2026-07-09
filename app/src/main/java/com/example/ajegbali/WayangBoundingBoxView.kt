package com.example.ajegbali

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View

/**
 * Custom View untuk render bounding boxes dengan perhitungan Aspect Ratio
 * yang akurat (Mengadopsi logika FIT_CENTER dari Jetpack Compose).
 */
class WayangBoundingBoxView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var detections = listOf<WayangDetectionResult>()

    // Variabel untuk menyimpan ukuran asli frame kamera
    private var frameWidth: Int = 1
    private var frameHeight: Int = 1

    private val boxPaint = Paint().apply {
        color = Color.rgb(255, 215, 0) // Gold Primary
        style = Paint.Style.STROKE
        strokeWidth = 6f
        isAntiAlias = true
    }

    private val textBgPaint = Paint().apply {
        color = Color.argb(210, 0, 0, 0) // Hitam transparan (85% alpha)
        style = Paint.Style.FILL
    }

    private val textPaint = Paint().apply {
        color = Color.rgb(255, 215, 0) // Gold Primary
        textSize = 36f
        isAntiAlias = true
        typeface = Typeface.DEFAULT_BOLD
    }

    // Fungsi untuk menerima hasil deteksi beserta ukuran gambar asli
    fun setDetections(detectionResults: List<WayangDetectionResult>, frameW: Int, frameH: Int) {
        detections = detectionResults
        frameWidth = if (frameW > 0) frameW else 1
        frameHeight = if (frameH > 0) frameH else 1
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (detections.isEmpty()) return

        val screenWidth = canvas.width.toFloat()
        val screenHeight = canvas.height.toFloat()

        // 1. Hitung rasio layar dan rasio kamera (Diadopsi dari kode teman Anda)
        val screenAspect = screenWidth / screenHeight
        val frameAspect = frameWidth.toFloat() / frameHeight.toFloat()

        val imageWidth: Float
        val imageHeight: Float

        // 2. Tentukan ukuran area gambar yang sebenarnya tampil di layar (FIT_CENTER)
        if (frameAspect > screenAspect) {
            // Gambar lebih lebar dari layar -> paskan ke lebar layar
            imageWidth = screenWidth
            imageHeight = screenWidth / frameAspect
        } else {
            // Gambar lebih tinggi dari layar -> paskan ke tinggi layar
            imageWidth = screenHeight * frameAspect
            imageHeight = screenHeight
        }

        // 3. Hitung offset (ruang kosong hitam di pinggir gambar)
        val offsetX = (screenWidth - imageWidth) / 2f
        val offsetY = (screenHeight - imageHeight) / 2f

        // 4. Gambar Bounding Box
        for (detection in detections) {
            val box = detection.boundingBox

            // Hitung koordinat presisi berdasarkan area gambar yang aktif
            val left = offsetX + (box.left * imageWidth)
            val top = offsetY + (box.top * imageHeight)
            val right = offsetX + (box.right * imageWidth)
            val bottom = offsetY + (box.bottom * imageHeight)

            // Gambar Kotak Utama
            canvas.drawRect(left, top, right, bottom, boxPaint)

            // Gambar Ornamen Siku (Corner brackets - Diadopsi dari BoundingBoxOverlay.kt)
            val cornerLen = minOf(right - left, bottom - top) * 0.15f
            boxPaint.strokeWidth = 10f
            // Kiri Atas
            canvas.drawLine(left, top, left + cornerLen, top, boxPaint)
            canvas.drawLine(left, top, left, top + cornerLen, boxPaint)
            // Kanan Atas
            canvas.drawLine(right, top, right - cornerLen, top, boxPaint)
            canvas.drawLine(right, top, right, top + cornerLen, boxPaint)
            // Kiri Bawah
            canvas.drawLine(left, bottom, left + cornerLen, bottom, boxPaint)
            canvas.drawLine(left, bottom, left, bottom - cornerLen, boxPaint)
            // Kanan Bawah
            canvas.drawLine(right, bottom, right - cornerLen, bottom, boxPaint)
            canvas.drawLine(right, bottom, right, bottom - cornerLen, boxPaint)
            boxPaint.strokeWidth = 6f // Kembalikan ketebalan

            // Gambar Label Teks
            val labelText = "${detection.characterName} ${(detection.confidence * 100).toInt()}%"
            val textWidth = textPaint.measureText(labelText)
            val textHeight = textPaint.textSize

            val labelY = (top - 16f).coerceAtLeast(textHeight + 16f)

            // Background Teks
            canvas.drawRoundRect(
                left, labelY - textHeight - 8f,
                left + textWidth + 16f, labelY + 8f,
                8f, 8f, textBgPaint
            )

            // Tulisan
            canvas.drawText(labelText, left + 8f, labelY, textPaint)
        }
    }
}