package com.example.antiphishingapp.feature.repository

import com.example.antiphishingapp.feature.model.AnalysisResponse
import com.example.antiphishingapp.network.ApiClient
import okhttp3.MultipartBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class AnalysisRepository {

    // ── 기존 API 서버용 (주석처리) ────────────────────────────────
    // private val api = ApiClient.apiService

    // ✅ AI 서버용으로 변경
    private val api = ApiClient.aiApiService

    // ── 일반 분석 요청 ────────────────────────────────────────────
    fun analyzeDocument(
        file: MultipartBody.Part,
        onResult: (AnalysisResponse?) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        try {
            api.processRequest(file).enqueue(object : Callback<AnalysisResponse> {
                override fun onResponse(
                    call: Call<AnalysisResponse>,
                    response: Response<AnalysisResponse>
                ) {
                    if (response.isSuccessful) {
                        onResult(response.body())
                    } else {
                        onError(Exception("서버 오류: ${response.code()}"))
                    }
                }

                override fun onFailure(call: Call<AnalysisResponse>, t: Throwable) {
                    onError(t)
                }
            })
        } catch (e: Exception) {
            onError(e)
        }
    }

    // ── 강제 분석 요청 (문서 판별 건너뜀, force=true) ─────────────
    fun analyzeDocumentForce(
        file: MultipartBody.Part,
        onResult: (AnalysisResponse?) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        try {
            api.processRequestForce(file).enqueue(object : Callback<AnalysisResponse> {
                override fun onResponse(
                    call: Call<AnalysisResponse>,
                    response: Response<AnalysisResponse>
                ) {
                    if (response.isSuccessful) {
                        onResult(response.body())
                    } else {
                        onError(Exception("서버 오류: ${response.code()}"))
                    }
                }

                override fun onFailure(call: Call<AnalysisResponse>, t: Throwable) {
                    onError(t)
                }
            })
        } catch (e: Exception) {
            onError(e)
        }
    }
}