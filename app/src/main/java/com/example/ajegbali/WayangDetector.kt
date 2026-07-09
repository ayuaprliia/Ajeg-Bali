package com.example.ajegbali

import android.content.Context
import android.graphics.Bitmap
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

/**
 * YOLOv11 TFLite inference engine untuk deteksi wayang kulit Bali.
 *
 * Spesifikasi Model:
 * - Input:  (1, 640, 640, 3) Float32 NHWC, normalized /255.0
 * - Output: (1, 20, 8400) di mana 20 = 4 bbox coords (cx,cy,w,h) + 16 class scores
 * - Labels: 16 karakter wayang Bali (urutan alfabetis)
 *
 * Pipeline:
 *   Bitmap → Letterbox(640x640) → Float32 Buffer → TFLite → Parse(1,20,8400)
 *   → Filter Confidence → NMS → Reverse Letterbox → List<WayangDetectionResult>
 */
class WayangDetector(context: Context) {

    private val interpreter: Interpreter
    private val labels: List<String>
    private val inputSize = 640

    companion object {
        const val DEFAULT_CONFIDENCE_THRESHOLD = 0.50f
        const val DEFAULT_IOU_THRESHOLD = 0.45f
        const val BBOX_COORDS = 4
        const val NUM_CLASSES = 16
        const val NUM_PREDICTIONS = 8400
        const val MODEL_FILE = "model_wayang.tflite"
        const val LABELS_FILE = "labels_wayang.txt"
    }

    init {
        try {
            val model = loadModelFile(context, MODEL_FILE)
            val options = Interpreter.Options().apply {
                setNumThreads(4)
            }
            interpreter = Interpreter(model, options)
            labels = loadLabels(context, LABELS_FILE)
        } catch (e: Exception) {
            throw RuntimeException("Gagal memuat model YOLOv11: ${e.message}", e)
        }
    }

    fun detect(
        bitmap: Bitmap,
        confidenceThreshold: Float = DEFAULT_CONFIDENCE_THRESHOLD,
        iouThreshold: Float = DEFAULT_IOU_THRESHOLD
    ): List<WayangDetectionResult> {

        // 1. Proses Letterboxing
        val (letterboxed, lbInfo) = WayangImageProcessor.letterboxBitmap(bitmap, inputSize)
        val inputBuffer = WayangImageProcessor.bitmapToByteBuffer(letterboxed, inputSize, inputSize)
        letterboxed.recycle()

        // 2. Siapkan Buffer Output
        val numRows = BBOX_COORDS + NUM_CLASSES
        val numPred = NUM_PREDICTIONS
        val outputBuffer = ByteBuffer.allocateDirect(1 * numRows * numPred * 4)
        outputBuffer.order(ByteOrder.nativeOrder())

        // 3. Jalankan TFLite
        interpreter.run(inputBuffer, outputBuffer)

        // 4. Parsing Output
        outputBuffer.rewind()
        val outputArray = FloatArray(numRows * numPred)
        outputBuffer.asFloatBuffer().get(outputArray)

        val rawDetections = mutableListOf<RawWayangDetection>()

        for (j in 0 until numPred) {
            val cx = outputArray[0 * numPred + j]
            val cy = outputArray[1 * numPred + j]
            val w  = outputArray[2 * numPred + j]
            val h  = outputArray[3 * numPred + j]

            var maxScore = 0f
            var maxClassIdx = 0
            for (c in 0 until NUM_CLASSES) {
                val score = outputArray[(BBOX_COORDS + c) * numPred + j]
                if (score > maxScore) {
                    maxScore = score
                    maxClassIdx = c
                }
            }

            if (maxScore >= confidenceThreshold) {
                rawDetections.add(
                    RawWayangDetection(
                        left = cx - w / 2f,
                        top = cy - h / 2f,
                        right = cx + w / 2f,
                        bottom = cy + h / 2f,
                        confidence = maxScore,
                        classIdx = maxClassIdx,
                        className = if (maxClassIdx < labels.size) labels[maxClassIdx] else "Unknown"
                    )
                )
            }
        }

        // 5. Non-Maximum Suppression
        val nmsResults = applyNMS(rawDetections, iouThreshold)

        // 6. Reverse Letterboxing & Mapping dengan WayangRepository
        return nmsResults.mapNotNull { raw ->
            val character = WayangRepository.getByClassId(raw.classIdx)

            if (character != null) {
                // Rumus Reverse Letterbox (Menghilangkan padding agar pas ke rasio asli)
                val left   = ((raw.left - lbInfo.padLeft) / lbInfo.scaledWidth).coerceIn(0f, 1f)
                val top    = ((raw.top - lbInfo.padTop) / lbInfo.scaledHeight).coerceIn(0f, 1f)
                val right  = ((raw.right - lbInfo.padLeft) / lbInfo.scaledWidth).coerceIn(0f, 1f)
                val bottom = ((raw.bottom - lbInfo.padTop) / lbInfo.scaledHeight).coerceIn(0f, 1f)

                WayangDetectionResult(
                    characterId = character.id, // Penambahan parameter wajib untuk fitur Capture
                    className = character.name,
                    characterName = character.name,
                    confidence = raw.confidence,
                    boundingBox = WayangBoundingBox(left, top, right, bottom)
                )
            } else {
                null
            }
        }
    }

    private fun applyNMS(
        detections: List<RawWayangDetection>,
        iouThreshold: Float
    ): List<RawWayangDetection> {
        if (detections.isEmpty()) return emptyList()

        val sorted = detections.sortedByDescending { it.confidence }
        val selected = mutableListOf<RawWayangDetection>()
        val suppressed = BooleanArray(sorted.size)

        for (i in sorted.indices) {
            if (suppressed[i]) continue
            selected.add(sorted[i])

            for (j in i + 1 until sorted.size) {
                if (suppressed[j]) continue
                if (calculateIoU(sorted[i], sorted[j]) > iouThreshold) {
                    suppressed[j] = true
                }
            }
        }

        return selected
    }

    private fun calculateIoU(a: RawWayangDetection, b: RawWayangDetection): Float {
        val intersectLeft = maxOf(a.left, b.left)
        val intersectTop = maxOf(a.top, b.top)
        val intersectRight = minOf(a.right, b.right)
        val intersectBottom = minOf(a.bottom, b.bottom)

        val intersectWidth = maxOf(0f, intersectRight - intersectLeft)
        val intersectHeight = maxOf(0f, intersectBottom - intersectTop)
        val intersectArea = intersectWidth * intersectHeight

        val aArea = (a.right - a.left) * (a.bottom - a.top)
        val bArea = (b.right - b.left) * (b.bottom - b.top)
        val unionArea = aArea + bArea - intersectArea

        return if (unionArea > 0f) intersectArea / unionArea else 0f
    }

    fun close() {
        interpreter.close()
    }

    private fun loadModelFile(context: Context, filename: String): MappedByteBuffer {
        return try {
            val assetFd = context.assets.openFd(filename)
            val inputStream = FileInputStream(assetFd.fileDescriptor)
            val fileChannel = inputStream.channel
            fileChannel.map(
                FileChannel.MapMode.READ_ONLY,
                assetFd.startOffset,
                assetFd.declaredLength
            )
        } catch (e: Exception) {
            throw RuntimeException("Gagal load model file: $filename", e)
        }
    }

    // Perbaikan Daftar Karakter Bali
    private fun loadLabels(context: Context, filename: String): List<String> {
        return try {
            context.assets.open(filename).bufferedReader().readLines()
        } catch (e: Exception) {
            listOf(
                "Acintya", "Arjuna", "Bhatara Siwa", "Bima",
                "Delem", "Durga", "Duryodana", "Krisna",
                "Kunti", "Madri", "Merdah", "Nakula-Sadewa",
                "Sangut", "Sengkuni", "Tualen", "Yudhistira"
            )
        }
    }
}

data class WayangBoundingBox(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
) {
    fun getWidth() = right - left
    fun getHeight() = bottom - top
}

data class WayangDetectionResult(
    val characterId: String, // <- PENTING: Ditambahkan agar sinkron dengan Intent
    val className: String,
    val characterName: String,
    val confidence: Float,
    val boundingBox: WayangBoundingBox
)

data class RawWayangDetection(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
    val confidence: Float,
    val classIdx: Int,
    val className: String
)