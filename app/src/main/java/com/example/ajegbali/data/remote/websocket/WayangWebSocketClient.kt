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
        .build()
    private val gson = Gson()

    fun connect() {
        val request = Request.Builder().url(url).build()
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val jsonResponse = gson.fromJson(text, JsonObject::class.java)
                    if (jsonResponse.has("error")) {
                        onError(jsonResponse.get("error").asString)
                        return
                    }

                    if (jsonResponse.has("result")) {
                        val resultObj = jsonResponse.getAsJsonObject("result")
                        val detections = mutableListOf<WayangDetectionResult>()

                        // Standard raw YOLOv8 fields
                        if (resultObj.has("boxes")) {
                            val boxes = resultObj.getAsJsonArray("boxes")
                            val classIds = resultObj.getAsJsonArray("class_ids")
                            val classNames = resultObj.getAsJsonArray("class_names")
                            val scores = resultObj.getAsJsonArray("scores")

                            for (i in 0 until boxes.size()) {
                                val box = boxes.get(i).asJsonArray
                                val cx = box.get(0).asFloat
                                val cy = box.get(1).asFloat
                                val w = box.get(2).asFloat
                                val h = box.get(3).asFloat

                                // Scaling assumes backend 640x640 input if values > 1.1
                                val isAbsolute = cx > 1.1f || cy > 1.1f || w > 1.1f || h > 1.1f
                                val scale = if (isAbsolute) 640f else 1.0f

                                val left = (cx - w / 2f) / scale
                                val top = (cy - h / 2f) / scale
                                val right = (cx + w / 2f) / scale
                                val bottom = (cy + h / 2f) / scale

                                detections.add(
                                    WayangDetectionResult(
                                        classIdx = classIds.get(i).asInt,
                                        characterName = classNames.get(i).asString,
                                        confidence = scores.get(i).asFloat,
                                        boundingBox = RectF(
                                            left.coerceIn(0f, 1f),
                                            top.coerceIn(0f, 1f),
                                            right.coerceIn(0f, 1f),
                                            bottom.coerceIn(0f, 1f)
                                        )
                                    )
                                )
                            }
                        }
                        onResult(detections)
                    }
                } catch (e: Exception) {
                    Log.e("WebSocketClient", "Error parsing message", e)
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                onError(t.message ?: "Connection failure")
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(1000, null)
            }
        })
    }

    fun sendImage(base64Image: String) {
        val json = JsonObject()
        json.addProperty("image", base64Image)
        webSocket?.send(gson.toJson(json))
    }

    fun close() {
        webSocket?.close(1000, "Normal closure")
    }
}
