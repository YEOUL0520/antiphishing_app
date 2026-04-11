package com.example.antiphishingapp.feature.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.antiphishingapp.feature.model.VoiceAnalysisResponse
import com.example.antiphishingapp.feature.model.VoiceUiResult
import com.example.antiphishingapp.feature.model.SuspiciousItem
import com.example.antiphishingapp.feature.repository.VoiceRepository
import kotlinx.coroutines.launch
import java.io.File

class VoiceAnalysisViewModel : ViewModel() {

    private val repository = VoiceRepository()

    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> get() = _loading

    private val _result = MutableLiveData<VoiceUiResult?>()
    val result: LiveData<VoiceUiResult?> get() = _result

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> get() = _error


    fun analyzeVoice(file: File) {

        val totalStartTime = System.currentTimeMillis()
        android.util.Log.d("VoicePerformance", "[PERF] 0. TOTAL_PROCESS_START: $totalStartTime")

        _loading.value = true
        _error.value = null

        viewModelScope.launch {
            repository.uploadVoiceFile(
                file = file,

                startTime = totalStartTime,
                onResult = { response, startTime ->
                    
                    _loading.postValue(false)

                    if (response == null) {
                        _error.postValue("서버 응답 없음")
                        return@uploadVoiceFile
                    }

                    val logicStartTime = System.currentTimeMillis()
                    val uiResult = convertToUiResult(response)
                    val logicEndTime = System.currentTimeMillis()
                    val warningLatency = logicEndTime - logicStartTime
                    
                    android.util.Log.d("VoicePerformance", "[RESULT] 통화 녹음본 위험 경고 시간: ${warningLatency}ms")

                    val totalEndTime = System.currentTimeMillis()
                    val endToEndLatency = totalEndTime - startTime
                    android.util.Log.d("VoicePerformance", "[RESULT] 통화 녹음본 End-to-End 지연: ${endToEndLatency}ms")
                    
                    _result.postValue(uiResult)
                },
                onError = { err ->
                    _loading.postValue(false)
                    _error.postValue(err.message ?: "알 수 없는 오류 발생")
                }
            )
        }
    }

    /**
     * 서버 JSON 데이터 → UI용 데이터 변환
     */
    private fun convertToUiResult(res: VoiceAnalysisResponse): VoiceUiResult {

        // 1️⃣ 위험도 계산
        val lvl = res.phishing_analysis.immediate.level * 25
        val mlScore = (res.phishing_analysis.comprehensive.confidence * 100).toInt()
        val riskScore = maxOf(lvl, mlScore)

        // 2️⃣ 의심 항목 자동 생성
        val suspiciousMessages = mutableListOf<String>()
        val transcript = res.transcription.text

        if (res.phishing_analysis.immediate.level > 0) {
            suspiciousMessages.add("보이스피싱 주요 키워드가 포함되었습니다.")
        }

        if (mlScore > 30) {
            suspiciousMessages.add("텍스트 분석 모델에서 위험 확률이 높게 감지되었습니다.")
        }

        if (Regex("검찰|금융|계좌|비밀번호|송금|즉시|긴급").containsMatchIn(transcript)) {
            suspiciousMessages.add("금융기관 사칭 또는 개인정보 요구 발화가 포함되었습니다.")
        }

        if (Regex("빨리|지금 바로|당장").containsMatchIn(transcript)) {
            suspiciousMessages.add("상대방이 즉각적인 행동을 요구하는 발화가 포함되어 있습니다.")
        }

        // 최소 1개는 넣어주기 (UX적으로 공백 방지)
        if (suspiciousMessages.isEmpty()) {
            suspiciousMessages.add("주요 의심 징후가 탐지되지 않았습니다.")
        }

        // 문자열 리스트 → SuspiciousItem 리스트로 변환 (중요!)
        val suspiciousItems = suspiciousMessages.map { SuspiciousItem(it) }

        return VoiceUiResult(
            riskScore = riskScore.coerceIn(0, 100),
            suspiciousItems = suspiciousItems,
            transcript = transcript
        )
    }

    fun resetResult() {
        _result.value = null
    }
}
