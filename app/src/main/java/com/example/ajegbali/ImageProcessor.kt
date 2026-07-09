package com.example.ajegbali

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import androidx.camera.core.ImageProxy
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Letterbox metadata — tracks how the original image was scaled and padded
 * into the model's 640×640 input. Used to reverse-map detection coordinates
 * back to original image space.
 */
data class WayangLetterboxInfo(
    val scale: Float,        // Scale factor applied to original image
    val padLeft: Float,      // Horizontal padding added (left side)
    val padTop: Float,       // Vertical padding added (top side)
    val scaledWidth: Float,  // Width of actual image content in 640x640
    val scaledHeight: Float  // Height of actual image content in 640x640
)

object WayangImageProcessor {

    fun letterboxBitmap(bitmap: Bitmap, targetSize: Int): Pair<Bitmap, WayangLetterboxInfo> {
        val srcW = bitmap.width.toFloat()
        val srcH = bitmap.height.toFloat()

        val scale = minOf(targetSize / srcW, targetSize / srcH)
        val scaledW = (srcW * scale)
        val scaledH = (srcH * scale)
        val padLeft = (targetSize - scaledW) / 2f
        val padTop = (targetSize - scaledH) / 2f

        val letterboxed = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(letterboxed)
        canvas.drawColor(Color.rgb(114, 114, 114))

        val resized = Bitmap.createScaledBitmap(bitmap, scaledW.toInt(), scaledH.toInt(), true)
        canvas.drawBitmap(resized, padLeft, padTop, null)
        if (resized !== bitmap) resized.recycle()

        val info = WayangLetterboxInfo(scale, padLeft, padTop, scaledW, scaledH)
        return Pair(letterboxed, info)
    }

    fun bitmapToByteBuffer(bitmap: Bitmap, targetWidth: Int, targetHeight: Int): ByteBuffer {
        val bufferSize = 1 * targetWidth * targetHeight * 3 * 4
        val buffer = ByteBuffer.allocateDirect(bufferSize)
        buffer.order(ByteOrder.nativeOrder())

        val pixels = IntArray(targetWidth * targetHeight)
        bitmap.getPixels(pixels, 0, targetWidth, 0, 0, targetWidth, targetHeight)

        for (pixel in pixels) {
            buffer.putFloat(((pixel shr 16) and 0xFF) / 255.0f) // R
            buffer.putFloat(((pixel shr 8) and 0xFF) / 255.0f)  // G
            buffer.putFloat((pixel and 0xFF) / 255.0f)           // B
        }

        buffer.rewind()
        return buffer
    }

    /**
     * FUNGSI DIPERBAIKI: Menggunakan toBitmap() bawaan CameraX.
     * Ini menjamin warna yang akurat dan rotasi otomatis yang benar.
     */
    fun imageProxyToBitmap(imageProxy: ImageProxy): Bitmap {
        // 1. Convert langsung dari ImageProxy ke Bitmap dengan CameraX API
        val bitmap = imageProxy.toBitmap()

        // 2. Koreksi rotasi jika diperlukan
        val rotation = imageProxy.imageInfo.rotationDegrees
        if (rotation != 0) {
            val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
            val rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            if (rotatedBitmap != bitmap) bitmap.recycle()
            return rotatedBitmap
        }

        return bitmap
    }
}