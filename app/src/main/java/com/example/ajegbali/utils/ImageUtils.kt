package com.example.ajegbali.utils

import android.graphics.*
import android.util.Base64
import androidx.camera.core.ImageProxy
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

object ImageUtils {

    fun imageProxyToBitmap(image: ImageProxy): Bitmap {
        val nv21 = yuv420888ToNv21(image)
        val yuvImage = YuvImage(nv21, ImageFormat.NV21, image.width, image.height, null)
        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, yuvImage.width, yuvImage.height), 100, out)
        val imageBytes = out.toByteArray()
        val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            ?: throw NullPointerException("Failed to decode YUV to Bitmap")
        
        // Handle rotation if needed
        val matrix = Matrix()
        matrix.postRotate(image.imageInfo.rotationDegrees.toFloat())
        
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun yuv420888ToNv21(image: ImageProxy): ByteArray {
        val width = image.width
        val height = image.height
        val ySize = width * height
        val uvSize = width * height / 2

        val nv21 = ByteArray(ySize + uvSize)

        val yPlane = image.planes[0].buffer
        val uPlane = image.planes[1].buffer
        val vPlane = image.planes[2].buffer

        var pos = 0

        // Y plane
        if (image.planes[0].rowStride == width) {
            yPlane.get(nv21, 0, ySize)
            pos = ySize
        } else {
            for (row in 0 until height) {
                yPlane.position(row * image.planes[0].rowStride)
                yPlane.get(nv21, pos, width)
                pos += width
            }
        }

        // UV plane
        val uRowStride = image.planes[1].rowStride
        val vRowStride = image.planes[2].rowStride
        val uvPixelStride = image.planes[1].pixelStride

        for (row in 0 until height / 2) {
            for (col in 0 until width / 2) {
                val uPos = row * uRowStride + col * uvPixelStride
                val vPos = row * vRowStride + col * uvPixelStride
                nv21[pos++] = vPlane.get(vPos)
                nv21[pos++] = uPlane.get(uPos)
            }
        }

        return nv21
    }

    fun bitmapToBase64(bitmap: Bitmap, quality: Int = 50): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        val byteArray = outputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }
}
