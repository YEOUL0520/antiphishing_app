package com.example.antiphishingapp.network

import com.example.antiphishingapp.feature.model.SignupRequest
import com.example.antiphishingapp.feature.model.UserResponse
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import retrofit2.Response

object ApiClient {

    // ✅ 서버 주소
//    const val BASE_URL = "https://gupi99.p-e.kr/"
//    const val AI_BASE_URL = BASE_URL
    const val BASE_URL = "http://10.0.2.2:8000"
    const val AI_BASE_URL = "http://10.0.2.2:8001"

    val WS_BASE_URL: String
        get() = when {
            BASE_URL.startsWith("https://") -> BASE_URL.replaceFirst("https://", "wss://")
            BASE_URL.startsWith("http://") -> BASE_URL.replaceFirst("http://", "ws://")
            else -> BASE_URL
        }

    fun wsUrl(path: String): String {
        val base = WS_BASE_URL.removeSuffix("/")
        val cleanPath = path.removePrefix("/")
        return "$base/$cleanPath"
    }

    val TRANSCRIPTION_WS_URL: String
        get() = wsUrl("api/transcribe/ws?sr=16000&lang=ko-KR")

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    val sharedClient: OkHttpClient
        get() = okHttpClient

    // ✅ API 서버용 Retrofit (인증 담당)
    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    // ✅ AI 서버용 Retrofit (위조 탐지 담당)
    private val aiRetrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(AI_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val apiService: ApiService by lazy {
        retrofit.create(ApiService::class.java)
    }

    // ✅ AI 서버용 ApiService
    val aiApiService: ApiService by lazy {
        aiRetrofit.create(ApiService::class.java)
    }

    suspend fun signup(request: SignupRequest): Response<UserResponse> {
        return apiService.signup(request)
    }
}