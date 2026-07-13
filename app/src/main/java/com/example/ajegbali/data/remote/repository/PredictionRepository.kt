package com.example.ajegbali.data.remote.repository

import com.example.ajegbali.data.remote.retrofit.ApiService
import com.example.ajegbali.data.remote.response.PredictionResponse
import com.example.ajegbali.data.Result
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

class PredictionRepository private constructor(
    private val apiService: ApiService
) {

    suspend fun classifyKetupat(imageFile: File): Result<PredictionResponse> {
        return try {
            val requestImageFile = imageFile.asRequestBody("image/jpeg".toMediaType())
            val multipartBody = MultipartBody.Part.createFormData("file", imageFile.name, requestImageFile)
            val response = apiService.classifyKetupat(multipartBody)
            if (response.isSuccess) {
                Result.Success(response)
            } else {
                Result.Error(response.errorMessage ?: "Classification failed")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Unknown error occurred")
        }
    }

    suspend fun classifyJejahitan(imageFile: File): Result<PredictionResponse> {
        return try {
            val requestImageFile = imageFile.asRequestBody("image/jpeg".toMediaType())
            val multipartBody = MultipartBody.Part.createFormData("file", imageFile.name, requestImageFile)
            val response = apiService.classifyJejahitan(multipartBody)
            if (response.isSuccess) {
                Result.Success(response)
            } else {
                Result.Error(response.errorMessage ?: "Classification failed")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Unknown error occurred")
        }
    }

    suspend fun detectWayang(imageFile: File): Result<PredictionResponse> {
        return try {
            val requestImageFile = imageFile.asRequestBody("image/jpeg".toMediaType())
            val multipartBody = MultipartBody.Part.createFormData("file", imageFile.name, requestImageFile)
            val response = apiService.detectWayang(multipartBody)
            
            // The Python implementation for predict_wayang doesn't return is_success explicitly,
            // but we added it to our response model with a default value of false.
            // Let's check boxes or detections as a fallback for success.
            if (response.isSuccess || response.boxes != null || response.detections != null) {
                Result.Success(response)
            } else {
                Result.Error(response.errorMessage ?: "Detection failed")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Unknown error occurred")
        }
    }

    companion object {
        @Volatile
        private var instance: PredictionRepository? = null
        fun getInstance(apiService: ApiService): PredictionRepository =
            instance ?: synchronized(this) {
                instance ?: PredictionRepository(apiService)
            }.also { instance = it }
    }
}
