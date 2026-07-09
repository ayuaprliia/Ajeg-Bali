package com.example.ajegbali

/**
 * Temporal smoothing untuk real-time detection — mengurangi flickering,
 * menstabilkan bounding boxes, dan memberikan label class yang konsisten.
 */
class WayangDetectionSmoother(
    private val maxAge: Int = 5,
    private val iouMatchThreshold: Float = 0.25f,
    private val emaAlpha: Float = 0.45f
) {
    private val tracks = mutableListOf<Track>()

    // TRACK DIPERBARUI: Menambahkan characterId dan characterName
    private data class Track(
        var characterId: String,
        var className: String,
        var characterName: String,
        var confidence: Float,
        var box: WayangBoundingBox,
        var age: Int = 0,
        var hits: Int = 1,
        val classVotes: MutableMap<String, Int> = mutableMapOf()
    )

    @Synchronized
    fun smooth(detections: List<WayangDetectionResult>): List<WayangDetectionResult> {
        // 1. Tambahkan umur track lama
        tracks.forEach { it.age++ }

        // 2. Cocokkan deteksi baru ke track yang ada
        val usedDet = mutableSetOf<Int>()
        val usedTrack = mutableSetOf<Int>()

        val candidates = mutableListOf<Triple<Int, Int, Float>>()
        for (ti in tracks.indices) {
            for (di in detections.indices) {
                val iou = boxIoU(tracks[ti].box, detections[di].boundingBox)
                if (iou > iouMatchThreshold) {
                    candidates.add(Triple(ti, di, iou))
                }
            }
        }

        candidates.sortByDescending { it.third }

        for ((ti, di, _) in candidates) {
            if (ti in usedTrack || di in usedDet) continue

            val track = tracks[ti]
            val det = detections[di]

            track.box = emaBox(track.box, det.boundingBox)
            track.confidence = track.confidence * (1 - emaAlpha) + det.confidence * emaAlpha

            track.classVotes[det.className] = (track.classVotes[det.className] ?: 0) + 1
            val bestEntry = track.classVotes.maxByOrNull { it.value }

            if (bestEntry != null) {
                track.className = bestEntry.key
                track.characterName = bestEntry.key
                // Sinkronkan ID jika namanya cocok
                if (det.className == bestEntry.key) {
                    track.characterId = det.characterId
                }
            }

            track.age = 0
            track.hits++

            usedDet.add(di)
            usedTrack.add(ti)
        }

        // 3. Buat track baru untuk deteksi yang tidak cocok
        for (di in detections.indices) {
            if (di !in usedDet) {
                val det = detections[di]
                tracks.add(
                    Track(
                        characterId = det.characterId,
                        className = det.className,
                        characterName = det.characterName,
                        confidence = det.confidence,
                        box = det.boundingBox,
                        classVotes = mutableMapOf(det.className to 1)
                    )
                )
            }
        }

        // 4. Hapus track yang sudah kedaluwarsa
        tracks.removeAll { it.age > maxAge }

        // 5. Kembalikan track yang aktif dengan penurunan confidence (decay)
        return tracks.mapNotNull { track ->
            val decayFactor = if (track.age == 0) 1f
            else (1f - track.age.toFloat() / (maxAge + 1).toFloat())

            val displayConf = track.confidence * decayFactor
            if (displayConf < 0.2f) return@mapNotNull null

            // KODE DIPERBAIKI: Memasukkan characterId ke hasil akhir
            WayangDetectionResult(
                characterId = track.characterId,
                className = track.className,
                characterName = track.characterName,
                confidence = displayConf,
                boundingBox = track.box
            )
        }
    }

    @Synchronized
    fun reset() {
        tracks.clear()
    }

    private fun emaBox(old: WayangBoundingBox, new: WayangBoundingBox): WayangBoundingBox {
        return WayangBoundingBox(
            left   = old.left   + emaAlpha * (new.left   - old.left),
            top    = old.top    + emaAlpha * (new.top    - old.top),
            right  = old.right  + emaAlpha * (new.right  - old.right),
            bottom = old.bottom + emaAlpha * (new.bottom - old.bottom)
        )
    }

    private fun boxIoU(a: WayangBoundingBox, b: WayangBoundingBox): Float {
        val interL = maxOf(a.left, b.left)
        val interT = maxOf(a.top, b.top)
        val interR = minOf(a.right, b.right)
        val interB = minOf(a.bottom, b.bottom)
        val interArea = maxOf(0f, interR - interL) * maxOf(0f, interB - interT)

        val aArea = (a.right - a.left) * (a.bottom - a.top)
        val bArea = (b.right - b.left) * (b.bottom - b.top)
        val union = aArea + bArea - interArea

        return if (union > 0f) interArea / union else 0f
    }
}