package com.example.ajegbali.utils

import android.content.Context
import android.graphics.*
import android.net.Uri
import android.util.Base64
import android.util.Log
import androidx.camera.core.ImageProxy
import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

object ImageUtils {

    /**
     * Converts CameraX ImageProxy to Bitmap with correct rotation.
     */
    fun imageProxyToBitmap(image: ImageProxy): Bitmap {
        val nv21 = yuv420888ToNv21(image)
        val yuvImage = YuvImage(nv21, ImageFormat.NV21, image.width, image.height, null)
        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, yuvImage.width, yuvImage.height), 100, out)
        val imageBytes = out.toByteArray()
        val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            ?: throw NullPointerException("Failed to decode YUV to Bitmap")
        
        val matrix = Matrix()
        matrix.postRotate(image.imageInfo.rotationDegrees.toFloat())
        
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    /**
     * Loads a Bitmap from URI and corrects its orientation based on EXIF.
     */
    fun getRotatedBitmapFromUri(context: Context, uri: Uri, maxDimension: Int = 1024): Bitmap? {
        return try {
            val contentResolver = context.contentResolver
            
            // Get dimensions first
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }

            // Calculate sample size
            var sampleSize = 1
            if (options.outWidth > maxDimension || options.outHeight > maxDimension) {
                val halfHeight = options.outHeight / 2
                val halfWidth = options.outWidth / 2
                while (halfHeight / sampleSize >= maxDimension && halfWidth / sampleSize >= maxDimension) {
                    sampleSize *= 2
                }
            }

            // Decode with sample size
            val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            val bitmap = contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, decodeOptions) } 
                ?: return null

            // Correct Rotation
            val inputStream: InputStream? = contentResolver.openInputStream(uri)
            val exif = inputStream?.use { ExifInterface(it) }
            val orientation = exif?.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
                ?: ExifInterface.ORIENTATION_NORMAL

            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1f, 1f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.preScale(1f, -1f)
            }

            if (matrix.isIdentity) {
                bitmap
            } else {
                val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                bitmap.recycle()
                rotated
            }
        } catch (e: Exception) {
            Log.e("ImageUtils", "Error loading rotated bitmap", e)
            null
        }
    }

    /**
     * Saves a Bitmap to a file.
     */
    fun saveBitmapToFile(bitmap: Bitmap, file: File, quality: Int = 90): Boolean {
        return try {
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
            }
            true
        } catch (e: Exception) {
            Log.e("ImageUtils", "Error saving bitmap to file", e)
            false
        }
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
