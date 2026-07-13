package com.example.ajegbali.data.remote.retrofit

import com.example.ajegbali.data.remote.response.PredictionResponse
import okhttp3.MultipartBody
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface ApiService {
    @Multipart
    @POST("detect-wayang")
    suspend fun detectWayang(
        @Part file: MultipartBody.Part
    ): PredictionResponse

    @Multipart
    @POST("classify-ketupat")
    suspend fun classifyKetupat(
        @Part file: MultipartBody.Part
    ): PredictionResponse

    @Multipart
    @POST("classify-jejahitan")
    suspend fun classifyJejahitan(
        @Part file: MultipartBody.Part
    ): PredictionResponse
}
