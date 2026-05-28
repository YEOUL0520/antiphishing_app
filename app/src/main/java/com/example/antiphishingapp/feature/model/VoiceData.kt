package com.example.antiphishingapp.feature.model

data class VoiceAnalysisResponse(
    val transcription: Transcription,
    val phishing_analysis: PhishingAnalysis
)

data class Transcription(
    val text: String? = null,
    val confidence: Double? = null,
    val speaker: String? = null,
    val stt_result: SttResult? = null
)

/** Clova STT 응답 필드가 버전마다 달라질 수 있어 모두 옵션 처리 */
data class SttResult(
    val result: String? = null,
    val message: String? = null,
    val token: String? = null,
    val progress: Int? = null
)

data class PhishingAnalysis(
    val immediate: Immediate? = null,
    val comprehensive: Comprehensive? = null,
    val warning_message: String? = null,
    val error: String? = null
)

data class Immediate(
    val level: Int,
    val probability: Double,
    val phishing_type: String?,
    val keywords: List<String>,
    val method: String
)

data class Comprehensive(
    val is_phishing: Boolean,
    val confidence: Double,
    val method: String,
    val analyzed_length: Int
)
