package com.example.ajegbali.ml.wayang

import android.graphics.RectF

data class WayangDetectionResult(
    val classIdx: Int,
    val characterName: String,
    val confidence: Float,
    val boundingBox: RectF
)
