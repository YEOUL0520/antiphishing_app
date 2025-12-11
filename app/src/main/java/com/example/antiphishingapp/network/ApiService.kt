package com.example.antiphishingapp.network

import com.example.antiphishingapp.feature.model.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.*

// 문자 분석 요청/응답 모델
data class SmsDetectRequest(
    val sender_hash: String,
    val urls: List<String>,
    val texts: List<String>,
    val received_at: Long
)

data class SmsDetectResponse(
    val phishing_score: Double,
    val keywords_found: List<String>,
    val url_results: Map<String, Map<String, Any>>
)
interface ApiService {

    @GET("healthz")
    fun checkHealth(): Call<String>

    @Multipart
    @POST("upload-image")
    fun uploadImage(@Part file: MultipartBody.Part): Call<ResponseBody>

    @Multipart
    @POST("upload-images")
    fun uploadMultipleImages(@Part files: List<MultipartBody.Part>): Call<ResponseBody>

    @Multipart
    @POST("api/transcribe/upload")
    fun uploadAudioFile(@Part file: MultipartBody.Part): Call<ResponseBody>

    @GET("api/transcribe/status/{token}")
    fun getTranscribeStatus(@Path("token") token: String): Call<ResponseBody>

    @Multipart
    @POST("process-request")
    fun processRequest(@Part file: MultipartBody.Part): Call<AnalysisResponse>

    @POST("api/sms/detect_json")
    fun detectSmsJson(@Body payload: SmsDetectRequest): Call<SmsDetectResponse>

    @Multipart
    @POST("api/voice-phishing/analyze-audio")
    fun analyzeAudioFile(
        @Part media: MultipartBody.Part,
        @Part("language") language: RequestBody,
        @Part("analysis_method") method: RequestBody
    ): Call<ResponseBody>

    // 일반 로그인
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<TokenResponse>

    @POST("auth/signup")
    suspend fun signup(@Body request: SignupRequest): Response<UserResponse>

    // 사용자 정보 조회
    @GET("auth/me")
    suspend fun getMe(@Header("Authorization") token: String): Response<UserResponse>
}
