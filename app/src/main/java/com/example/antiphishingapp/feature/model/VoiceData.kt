package com.example.antiphishingapp.feature.model

data class VoiceAnalysisResponse(
    val transcription: Transcription = Transcription(),
    val phishing_analysis: PhishingAnalysis = PhishingAnalysis()
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
    val level: Int = 0,
    val probability: Double = 0.0,
    val phishing_type: String? = null,
    val keywords: List<String> = emptyList(),
    val method: String = "word_based"
)

data class Comprehensive(
    val is_phishing: Boolean = false,
    val confidence: Double = 0.0,
    val method: String = "tfidf_rf",
    val analyzed_length: Int = 0
)
