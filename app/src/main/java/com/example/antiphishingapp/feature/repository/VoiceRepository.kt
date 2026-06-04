package com.example.antiphishingapp.feature.repository

import com.example.antiphishingapp.feature.model.VoiceAnalysisResponse
import com.example.antiphishingapp.network.ApiClient
import com.example.antiphishingapp.utils.audioToMultipart
import android.util.Log
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * VoiceRepository
 * ------------------------
 * 1️⃣ 음성 파일을 Multipart 로 변환
 * 2️⃣ Retrofit 으로 서버 업로드 (/api/voice-phishing/analyze-audio)
 * 3️⃣ JSON → VoiceAnalysisResponse 로 자동 파싱
 * 4️⃣ ViewModel 로 결과 전달
 */
class VoiceRepository {

    private val api = ApiClient.aiApiService
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

            val call = api.analyzeAudioFile(mediaPart, langPart, methodPart)
            // 장시간 대기로 로딩이 끝나지 않는 상황을 방지
            call.timeout().timeout(125, TimeUnit.SECONDS)
            call.enqueue(object : Callback<ResponseBody> {

                    override fun onResponse(
                        call: Call<ResponseBody>,
                        response: Response<ResponseBody>
                    ) {
                        if (!response.isSuccessful) {
                            val errorDetail = response.errorBody()?.string()?.takeIf { it.isNotBlank() }
                            val message = if (errorDetail != null) {
                                "서버 오류(${response.code()}): $errorDetail"
                            } else {
                                "서버 오류: ${response.code()}"
                            }
                            onError(Exception(message))
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
                            val parsed = VoiceResponseParser.parse(jsonString)
                            Log.d("VoiceRepository", "analyze-audio parsed OK, text.len=${parsed.transcription.text?.length ?: 0}")
                            onResult(parsed, startTime)
                        } catch (e: Exception) {
                            Log.e("VoiceRepository", "JSON parse fail: ${e.message}, head=${jsonString.take(400)}")
                            onError(Exception("JSON 파싱 오류: ${e.message}"))
                        }
                    }

                    override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                        val msg = t.message ?: "알 수 없는 네트워크 오류"
                        // 타임아웃 케이스는 사용자에게 더 명확히 전달
                        if (msg.contains("timeout", ignoreCase = true)) {
                            onError(Exception("음성 분석 응답 대기 시간이 초과되었습니다. 파일 길이를 줄이거나 네트워크 상태를 확인해 주세요."))
                        } else {
                            onError(t)
                        }
                    }
                })

        } catch (e: Exception) {
            onError(e)
        }
    }
}
