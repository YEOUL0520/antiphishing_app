package com.example.antiphishingapp.network

import com.example.antiphishingapp.feature.model.SignupRequest
import com.example.antiphishingapp.feature.model.UserResponse
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import retrofit2.Response

object ApiClient {

    // ✅ 서버 기본 주소
    const val BASE_URL = "https://gupi99.p-e.kr"

    // ✅ WebSocket용 주소 자동 변환
    val WS_BASE_URL: String
        get() = when {
            BASE_URL.startsWith("https://") -> BASE_URL.replaceFirst("https://", "wss://")
            BASE_URL.startsWith("http://") -> BASE_URL.replaceFirst("http://", "ws://")
            else -> BASE_URL
        }

    // ✅ WebSocket URL Helper
    fun wsUrl(path: String): String {
        val base = WS_BASE_URL.removeSuffix("/")
        val cleanPath = path.removePrefix("/")
        return "$base/$cleanPath"
    }

    // ✅ STT 전용 WebSocket URL
    val TRANSCRIPTION_WS_URL: String
        get() = wsUrl("api/transcribe/ws?sr=16000&lang=ko-KR")

    // ✅ 내부용 OkHttpClient (private 유지)
    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    // ✅ 외부에서 재사용할 수 있는 getter (읽기 전용)
    val sharedClient: OkHttpClient
        get() = okHttpClient

    // ✅ Retrofit 인스턴스
    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val apiService: ApiService by lazy {
        retrofit.create(ApiService::class.java)
    }

    suspend fun signup(request: SignupRequest): Response<UserResponse> {
        return apiService.signup(request)
    }
}
