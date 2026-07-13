package com.example.ajegbali.data.remote.response

import com.google.gson.annotations.SerializedName

data class PredictionResponse(
    @SerializedName("is_success")
    val isSuccess: Boolean = false,
    
    @SerializedName("error_message")
    val errorMessage: String? = null,
    
    @SerializedName("prediction_type")
    val predictionType: String? = null,
    
    // Classification fields
    @SerializedName("class_id")
    val classId: Int? = null,
    
    @SerializedName("class_name")
    val className: String? = null,
    
    @SerializedName("confidence")
    val confidence: Float? = null,
    
    @SerializedName("class_scores")
    val classScores: List<Float>? = null,
    
    // Detection fields (YOLOv8 raw format)
    @SerializedName("boxes")
    val boxes: List<List<Float>>? = null,
    
    @SerializedName("scores")
    val scores: List<Float>? = null,
    
    @SerializedName("class_ids")
    val classIds: List<Int>? = null,
    
    @SerializedName("class_names")
    val classNames: List<String?>? = null,
    
    // Alternative Detection format (as seen in classify_ketupat)
    @SerializedName("detections")
    val detections: List<DetectionItem>? = null
)

data class DetectionItem(
    @SerializedName("box")
    val box: List<Float>? = null,
    
    @SerializedName("confidence")
    val confidence: Float? = null,
    
    @SerializedName("class_id")
    val classId: Int? = null,
    
    @SerializedName("class_name")
    val className: String? = null
)
