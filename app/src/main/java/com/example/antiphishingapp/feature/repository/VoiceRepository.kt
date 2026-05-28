package com.example.antiphishingapp.feature.repository

import com.example.antiphishingapp.feature.model.VoiceAnalysisResponse
import com.example.antiphishingapp.network.ApiClient
import com.example.antiphishingapp.utils.audioToMultipart
import com.google.gson.Gson
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File

/**
 * VoiceRepository
 * ------------------------
 * 1️⃣ 음성 파일을 Multipart 로 변환
 * 2️⃣ Retrofit 으로 서버 업로드 (/api/voice-phishing/analyze-audio)
 * 3️⃣ JSON → VoiceAnalysisResponse 로 자동 파싱
 * 4️⃣ ViewModel 로 결과 전달
 */
class VoiceRepository {

    private val api = ApiClient.apiService
    private val gson = Gson()

    fun uploadVoiceFile(
        file: File,
        language: String = "ko-KR",
        method: String = "hybrid",
        
        startTime: Long, 
        onResult: (VoiceAnalysisResponse?, Long) -> Unit,
        
        onError: (Throwable) -> Unit
    ) {
        try {
            val mediaPart = audioToMultipart(file)
            val langPart = language.toRequestBody("text/plain".toMediaTypeOrNull())
            val methodPart = method.toRequestBody("text/plain".toMediaTypeOrNull())

            val apiStartTime = System.currentTimeMillis()
            android.util.Log.d("VoicePerformance", "[PERF] 1. API_REQUEST_START: $apiStartTime")

            api.analyzeAudioFile(mediaPart, langPart, methodPart)
                .enqueue(object : Callback<ResponseBody> {

                    override fun onResponse(
                        call: Call<ResponseBody>,
                        response: Response<ResponseBody>
                    ) {
                        if (!response.isSuccessful) {
                            onError(Exception("서버 오류: ${response.code()}"))
                            return
                        }

                        val apiEndTime = System.currentTimeMillis()
                        val apiLatency = apiEndTime - apiStartTime
                        android.util.Log.d("VoicePerformance", "[PERF] 2. API_RESPONSE_RECEIVED")
                        android.util.Log.d("VoicePerformance", "[RESULT] 통화 녹음본 결과 수신 대기 시간: ${apiLatency}ms")

                        val jsonString = response.body()?.string()
                        if (jsonString == null) {
                            onError(Exception("서버 응답이 비어 있습니다."))
                            return
                        }

                        try {
                            // JSON → Data Class 변환
                            val parsed = gson.fromJson(
                                jsonString,
                                VoiceAnalysisResponse::class.java
                            )
                            onResult(parsed, startTime)

                        } catch (e: Exception) {
                            onError(Exception("JSON 파싱 오류: ${e.message}"))
                        }
                    }

                    override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                        onError(t)
                    }
                })

        } catch (e: Exception) {
            onError(e)
        }
    }
}
