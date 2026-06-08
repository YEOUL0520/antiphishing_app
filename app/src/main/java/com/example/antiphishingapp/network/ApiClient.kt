package com.example.antiphishingapp.network

import com.example.antiphishingapp.feature.model.SignupRequest
import com.example.antiphishingapp.feature.model.UserResponse
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import retrofit2.Response

object ApiClient {

    // ✅ API 서버(인증/문서) — AppConfig.BASE_URL 과 동일 호스트 권장
    private const val PROD_HOST = "https://gupi99.p-e.kr/"

    // 에뮬레이터 전용: PC localhost (실기기에서는 동작 안 함)
    private const val EMULATOR_API = "http://10.0.2.2:8000/"
    private const val EMULATOR_AI = "http://10.0.2.2:8001/"

    /** 실기기·운영 빌드: PROD_HOST / 에뮬레이터: EMULATOR_* 로 바꿔서 빌드 */
    const val BASE_URL = PROD_HOST

    // 음성 녹음 analyze-audio, STT WebSocket — nginx가 /api/* → ai_server(8001) 로 프록시해야 함
    const val AI_BASE_URL = PROD_HOST

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