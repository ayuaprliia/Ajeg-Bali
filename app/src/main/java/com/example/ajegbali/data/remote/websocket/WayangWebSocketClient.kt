package com.example.ajegbali.data.remote.websocket

import android.graphics.RectF
import android.util.Log
import com.example.ajegbali.ml.wayang.WayangDetectionResult
import com.google.gson.Gson
import com.google.gson.JsonObject
import okhttp3.*
import java.util.concurrent.TimeUnit

class WayangWebSocketClient(
    private val url: String,
    private val onResult: (List<WayangDetectionResult>) -> Unit,
    private val onError: (String) -> Unit
) {
    private var webSocket: WebSocket? = null
    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .connectTimeout(10, TimeUnit.SECONDS)
        .build()
    private val gson = Gson()

    fun connect() {
        Log.d("WayangWS", "Connecting to: $url")
        val request = Request.Builder().url(url).build()
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d("WayangWS", "WebSocket Opened")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d("WayangWS", "Message Received: ${text.take(100)}...")
                try {
                    val jsonResponse = gson.fromJson(text, JsonObject::class.java)
                    if (jsonResponse.has("error")) {
                        val errorMsg = jsonResponse.get("error").asString
                        Log.e("WayangWS", "Backend error: $errorMsg")
                        onError(errorMsg)
                        return
                    }

                    val detections = mutableListOf<WayangDetectionResult>()

                    // The backend sends results wrapped in a "result" key
                    val resultObj = when {
                        jsonResponse.has("result") -> jsonResponse.get("result")
                        else -> null
                    }

                    if (resultObj != null && resultObj.isJsonObject) {
                        val res = resultObj.asJsonObject
                        
                        // Format 1: detections list (preferred structured format)
                        if (res.has("detections")) {
                            res.getAsJsonArray("detections").forEach { item ->
                                val det = item.asJsonObject
                                val box = det.getAsJsonArray("box")
                                if (box != null && box.size() >= 4) {
                                    detections.add(
                                        WayangDetectionResult(
                                            classIdx = if (det.has("class_id")) det.get("class_id").asInt else 0,
                                            characterName = if (det.has("class_name")) det.get("class_name").asString else "Unknown",
                                            confidence = if (det.has("confidence")) det.get("confidence").asFloat else 0f,
                                            boundingBox = mapToNormalizedRect(
                                                listOf(box.get(0).asFloat, box.get(1).asFloat, box.get(2).asFloat, box.get(3).asFloat)
                                            )
                                        )
                                    )
                                }
                            }
                        } 
                        // Format 2: Raw YOLO fields
                        else if (res.has("boxes")) {
                            val boxes = res.getAsJsonArray("boxes")
                            val classNames = if (res.has("class_names")) res.getAsJsonArray("class_names") else null
                            val scores = if (res.has("scores")) res.getAsJsonArray("scores") else null
                            
                            for (i in 0 until boxes.size()) {
                                val boxArray = boxes.get(i).asJsonArray
                                if (boxArray.size() >= 4) {
                                    detections.add(
                                        WayangDetectionResult(
                                            classIdx = 0,
                                            characterName = classNames?.get(i)?.asString ?: "Unknown",
                                            confidence = scores?.get(i)?.asFloat ?: 0f,
                                            boundingBox = mapToNormalizedRect(
                                                listOf(boxArray.get(0).asFloat, boxArray.get(1).asFloat, boxArray.get(2).asFloat, boxArray.get(3).asFloat)
                                            )
                                        )
                                    )
                                }
                            }
                        }
                    }

                    Log.d("WayangWS", "Parsed ${detections.size} detections")
                    onResult(detections)
                    
                } catch (e: Exception) {
                    Log.e("WayangWS", "Error parsing message", e)
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e("WayangWS", "Connection Failure: ${t.message}", t)
                onError(t.message ?: "Connection failure")
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d("WayangWS", "WebSocket Closing: $reason")
                webSocket.close(1000, null)
            }
        })
    }

    private fun mapToNormalizedRect(box: List<Float>): RectF {
        val cx = box[0]
        val cy = box[1]
        val w = box[2]
        val h = box[3]
        
        val isAbsolute = box.any { it > 1.1f }
        val scale = if (isAbsolute) 640f else 1.0f

        return RectF(
            ((cx - w / 2f) / scale).coerceIn(0f, 1f),
            ((cy - h / 2f) / scale).coerceIn(0f, 1f),
            ((cx + w / 2f) / scale).coerceIn(0f, 1f),
            ((h + cy / 2f) / scale).coerceIn(0f, 1f) // Wait, h + cy/2? No, cx + w/2 and cy + h/2
        ).apply {
            // Re-correcting the math inline for standard xywh
            val l = (cx - w / 2f) / scale
            val t = (cy - h / 2f) / scale
            val r = (cx + w / 2f) / scale
            val b = (cy + h / 2f) / scale
            set(l.coerceIn(0f, 1f), t.coerceIn(0f, 1f), r.coerceIn(0f, 1f), b.coerceIn(0f, 1f))
        }
    }

    fun sendImage(base64Image: String) {
        if (webSocket == null) {
            Log.w("WayangWS", "Cannot send image: WebSocket is null")
            return
        }
        val json = JsonObject()
        json.addProperty("image", base64Image)
        val sent = webSocket?.send(gson.toJson(json)) == true
        if (!sent) Log.e("WayangWS", "Failed to send image data")
    }

    fun close() {
        webSocket?.close(1000, "Normal closure")
    }
}
