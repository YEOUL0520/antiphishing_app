package com.example.antiphishingapp.feature.repository

import com.example.antiphishingapp.feature.model.Comprehensive
import com.example.antiphishingapp.feature.model.Immediate
import com.example.antiphishingapp.feature.model.PhishingAnalysis
import com.example.antiphishingapp.feature.model.Transcription
import com.example.antiphishingapp.feature.model.VoiceAnalysisResponse
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser

/**
 * /analyze-audio 응답 파싱.
 * - 정상: { transcription, phishing_analysis }
 * - Clova stt_result 객체/배열 필드 제거 (Gson 타입 오류 방지)
 * - immediate/comprehensive null 시 기본값
 */
object VoiceResponseParser {

    private val gson = Gson()

    fun parse(jsonString: String): VoiceAnalysisResponse {
        val root = JsonParser.parseString(jsonString).asJsonObject
        sanitizeTranscription(root)

        if (root.has("phishing_analysis")) {
            return gson.fromJson(root, VoiceAnalysisResponse::class.java)
        }

        // 레거시/텍스트 analyze 형태: 최상위 immediate·comprehensive
        return VoiceAnalysisResponse(
            transcription = parseTranscription(root.get("transcription")),
            phishing_analysis = PhishingAnalysis(
                immediate = parseImmediate(root.get("immediate")),
                comprehensive = parseComprehensive(root.get("comprehensive")),
                warning_message = root.get("warning_message")?.asStringOrNull(),
                error = root.get("error")?.asStringOrNull()
            )
        )
    }

    private fun sanitizeTranscription(root: JsonObject) {
        val tr = root.getAsJsonObject("transcription") ?: return
        val stt = tr.get("stt_result") ?: return
        if (!stt.isJsonObject) {
            tr.remove("stt_result")
            return
        }
        val safe = JsonObject()
        stt.asJsonObject.entrySet().forEach { (key, value) ->
            when (key) {
                "result", "message", "token" -> if (value.isJsonPrimitive && value.asJsonPrimitive.isString) {
                    safe.addProperty(key, value.asString)
                }
                "progress" -> if (value.isJsonPrimitive && value.asJsonPrimitive.isNumber) {
                    safe.addProperty(key, value.asInt)
                }
            }
        }
        if (safe.size() > 0) tr.add("stt_result", safe) else tr.remove("stt_result")
    }

    private fun parseTranscription(el: JsonElement?): Transcription {
        if (el == null || !el.isJsonObject) return Transcription()
        return gson.fromJson(el, Transcription::class.java)
    }

    private fun parseImmediate(el: JsonElement?): Immediate? {
        if (el == null || !el.isJsonObject) return null
        return gson.fromJson(el, Immediate::class.java)
    }

    private fun parseComprehensive(el: JsonElement?): Comprehensive? {
        if (el == null || !el.isJsonObject) return null
        return gson.fromJson(el, Comprehensive::class.java)
    }

    private fun JsonElement.asStringOrNull(): String? =
        if (isJsonNull) null else asString
}
