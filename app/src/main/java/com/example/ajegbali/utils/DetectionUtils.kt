package com.example.ajegbali.utils

import android.graphics.RectF
import com.example.ajegbali.ml.wayang.WayangDetectionResult

object DetectionUtils {

    /**
     * Performs Non-Maximum Suppression (NMS) to remove redundant overlapping boxes.
     *
     * @param detections   List of all raw detections from the backend.
     * @param iouThreshold IoU threshold for suppression. Lower = more aggressive.
     *                     0.3 is a good default for wayang detection where objects don't
     *                     overlap — any box sharing more than 30% area with a better box
     *                     is considered a duplicate and removed.
     * @param minConfidence Detections below this score are discarded before NMS runs.
     *                      This eliminates noisy low-confidence predictions whose coordinates
     *                      differ just enough from the best box to survive a pure IoU filter.
     */
    fun performNMS(
        detections: List<WayangDetectionResult>,
        iouThreshold: Float = 0.3f,
        minConfidence: Float = 0.25f
    ): List<WayangDetectionResult> {
        if (detections.isEmpty()) return emptyList()

        // Pre-filter: drop anything below the confidence floor before NMS
        val candidates = detections
            .filter { it.confidence >= minConfidence }
            .sortedByDescending { it.confidence }
            .toMutableList()

        val results = mutableListOf<WayangDetectionResult>()

        while (candidates.isNotEmpty()) {
            val best = candidates.removeAt(0)
            results.add(best)

            val iterator = candidates.iterator()
            while (iterator.hasNext()) {
                val next = iterator.next()
                if (calculateIoU(best.boundingBox, next.boundingBox) > iouThreshold) {
                    iterator.remove()
                }
            }
        }

        return results
    }

    private fun calculateIoU(rect1: RectF, rect2: RectF): Float {
        val intersection = RectF()
        if (!intersection.setIntersect(rect1, rect2)) return 0f

        val intersectionArea = intersection.width() * intersection.height()
        val rect1Area = rect1.width() * rect1.height()
        val rect2Area = rect2.width() * rect2.height()

        return intersectionArea / (rect1Area + rect2Area - intersectionArea)
    }
}
