package com.example.myapp

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder   
import java.nio.channels.FileChannel

class KetupatClassifier(context: Context) {

    private var interpreter: Interpreter? = null
    private val imageSize = 224

    private val daftarKelas = arrayOf(
        "Bagia", "Dampulan", "Gatep", "Gong", "Kukur",
        "Lepet", "Lojor", "Nasi", "Sari", "Sidakarya",
        "Sidapurna", "Sirikan", "Taluh"
    )

    init {
        try {
            val assetManager = context.assets
            val fileDescriptor = assetManager.openFd("model_ketupat.tflite")
            val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
            val fileChannel = inputStream.channel
            val startOffset = fileDescriptor.startOffset
            val declaredLength = fileDescriptor.declaredLength

            val modelBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)

            val options = Interpreter.Options().apply {
                numThreads = 4
            }
            interpreter = Interpreter(modelBuffer, options)
            Log.d("KetupatClassifier", "MODEL BERHASIL DIMUAT!")
        } catch (e: Exception) {
            Log.e("KetupatClassifier", "GAGAL MEMUAT MODEL: ${e.message}")
            e.printStackTrace()
        }
    }

    fun klasifikasiGambar(bitmap: Bitmap): PrediksiHasil {
        if (interpreter == null) {
            return PrediksiHasil("Error (Model Tidak Ditemukan)", 0f, 0L)
        }

        val gambarResized = Bitmap.createScaledBitmap(bitmap, imageSize, imageSize, false)
        val byteBuffer = ByteBuffer.allocateDirect(4 * imageSize * imageSize * 3)
        byteBuffer.order(ByteOrder.nativeOrder())

        val intValues = IntArray(imageSize * imageSize)
        gambarResized.getPixels(intValues, 0, gambarResized.width, 0, 0, gambarResized.width, gambarResized.height)

        byteBuffer.rewind()

        var pixel = 0
        for (i in 0 until imageSize) {
            for (j in 0 until imageSize) {
                val valPixel = intValues[pixel++]

                val r = ((valPixel shr 16) and 0xFF).toFloat()
                val g = ((valPixel shr 8) and 0xFF).toFloat()
                val b = (valPixel and 0xFF).toFloat()

                byteBuffer.putFloat(b)
                byteBuffer.putFloat(g)
                byteBuffer.putFloat(r)
            }
        }

        val outputProbabilitas = Array(1) { FloatArray(daftarKelas.size) }

        // Hitung Waktu Inference
        val waktuMulai = System.currentTimeMillis()

        try {
            byteBuffer.rewind()
            interpreter?.run(byteBuffer, outputProbabilitas)
        } catch (e: Exception) {
            Log.e("KetupatClassifier", "INFERENCE GAGAL: ${e.message}")
            return PrediksiHasil("Inference Error", 0f, 0L)
        }

        val waktuSelesai = System.currentTimeMillis()
        val totalWaktuEksekusi = waktuSelesai - waktuMulai

        val hasil = outputProbabilitas[0]
        var maxConfidence = 0f
        var maxIndex = -1

        for (i in hasil.indices) {
            if (hasil[i] > maxConfidence) {
                maxConfidence = hasil[i]
                maxIndex = i
            }
        }

        return PrediksiHasil(
            label = if (maxIndex != -1) daftarKelas[maxIndex] else "Tidak Diketahui",
            confidence = maxConfidence,
            waktuEksekusi = totalWaktuEksekusi
        )
    }

    fun tutupModel() {
        interpreter?.close()
    }

    data class PrediksiHasil(val label: String, val confidence: Float, val waktuEksekusi: Long)
}