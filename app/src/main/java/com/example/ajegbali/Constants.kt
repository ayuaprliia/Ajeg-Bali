package com.example.ajegbali

/**
 * App-wide constants for WayangVision.
 */
object Constants {
    // ── UI / App ──
    const val DEFAULT_CONFIDENCE_THRESHOLD = 0.5f
    const val DEFAULT_INPUT_RESOLUTION = 640
    const val SPLASH_MIN_DURATION_MS = 2500L
    val RESOLUTION_OPTIONS = listOf(320, 416, 640)
    const val APP_VERSION = "v1.0.0"
    const val MODEL_NAME = "YOLOv11s-TFLite"

    // ── TFLite Model ──
    // SANGAT PENTING: Nama file sudah disesuaikan dengan file Anda di folder assets
    const val MODEL_FILE = "model_wayang.tflite"

    const val LABELS_FILE = "labels_wayang.txt"
    const val MODEL_INPUT_SIZE = 640
    const val DEFAULT_IOU_THRESHOLD = 0.45f

    // Konfigurasi model YOLOv11
    const val NUM_CLASSES = 16
    const val NUM_PREDICTIONS = 8400
    const val BBOX_COORDS = 4
}