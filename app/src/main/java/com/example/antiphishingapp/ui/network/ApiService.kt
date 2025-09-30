package com.example.antiphishingapp.network

import okhttp3.MultipartBody
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

// --- 박스 좌표 DTO ---
data class BoxDto(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int
)

// --- 서버 응답 모델: 직인 분석 ---
data class DetectionResponse(
    val filename: String,
    val url: String,
    val boxes: List<BoxDto>
)

// --- 서버 응답 모델: 업로드 ---
data class UploadResponse(
    val filename: String,
    val url: String
)

// --- Retrofit API 인터페이스 ---
interface ApiService {
    // ✅ 직인 분석 (나중에 서버에서 구현 예정)
    @Multipart
    @POST("analyze-stamp")
    fun analyzeStamp(
        @Part file: MultipartBody.Part
    ): Call<DetectionResponse>

    // ✅ 이미지 업로드 (이미 서버에 구현됨)
    @Multipart
    @POST("upload-image")
    fun uploadImage(
        @Part file: MultipartBody.Part
    ): Call<UploadResponse>
}

// --- Retrofit 클라이언트 ---
object RetrofitClient {
    private const val BASE_URL = "https://fapi-3llz.onrender.com/" // ⚠️ 끝에 '/' 필수

    val instance: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
