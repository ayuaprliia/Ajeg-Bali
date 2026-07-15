package com.example.ajegbali.data.remote.websocket

import android.graphics.RectF
import android.util.Log
import com.example.ajegbali.ml.wayang.WayangDetectionResult
import com.example.ajegbali.utils.DetectionUtils
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
        .connectTimeout(15, TimeUnit.SECONDS)
        .build()
    private val gson = Gson()

    fun connect() {
        Log.d("WayangWS", "Connecting to: $url")
        
        // Add Origin header to avoid 403 Forbidden on AWS App Runner/FastAPI
        val origin = url.replace("wss://", "https://").replace("ws://", "http://")
        
        val request = Request.Builder()
            .url(url)
            .addHeader("Origin", origin)
            .addHeader("User-Agent", "AjegBali-Android-App")
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d("WayangWS", "WebSocket Opened")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
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

                    Log.d("WayangWS", "Parsed ${detections.size} raw detections")
                    val filtered = DetectionUtils.performNMS(detections)
                    Log.d("WayangWS", "After NMS: ${filtered.size} detections")
                    onResult(filtered)
                    
                } catch (e: Exception) {
                    Log.e("WayangWS", "Error parsing message", e)
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                val code = response?.code
                Log.e("WayangWS", "Connection Failure: ${t.message} (Code: $code)", t)
                
                if (code == 403) {
                    onError("Forbidden (403): Check CORS or Origin settings on AWS App Runner.")
                } else {
                    onError(t.message ?: "Connection failure")
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d("WayangWS", "WebSocket Closing: $reason")
                webSocket.close(1000, null)
            }
        })
    }

    /**
     * Converts a [cx, cy, w, h] bounding box from the backend into a normalized [0, 1] RectF.
     */
    private fun mapToNormalizedRect(box: List<Float>): RectF {
        val cx = box[0]
        val cy = box[1]
        val w  = box[2]
        val h  = box[3]

        val scale = if (box.any { it > 1.1f }) 640f else 1.0f

        val left   = ((cx - w / 2f) / scale).coerceIn(0f, 1f)
        val top    = ((cy - h / 2f) / scale).coerceIn(0f, 1f)
        val right  = ((cx + w / 2f) / scale).coerceIn(0f, 1f)
        val bottom = ((cy + h / 2f) / scale).coerceIn(0f, 1f)

        return RectF(left, top, right, bottom)
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
