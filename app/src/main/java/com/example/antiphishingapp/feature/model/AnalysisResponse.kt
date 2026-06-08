package com.example.antiphishingapp.feature.model

data class AnalysisResponse(
    val filename: String,
    val url: String,
    val forgery: ForgeryInfo

    // ── 기존 응답 구조 (주석처리) ──────────────────────────────────
    // val stamp: StampInfo,
    // val keyword: KeywordInfo,
    // val layout: LayoutInfo,
    // val final_risk: Float
)

data class ForgeryInfo(
    val document_detected: Boolean = true,  // ── 추가: 문서 판별 결과 (false면 문서 아님)
    val was_cropped: Boolean = false,        // ── 추가: 크롭 발생 여부
    val is_forged: Boolean?,                // ── nullable로 변경: 문서 아님일 때 null
    val result: String,                     // "정상" / "위조 의심" / "문서 아님"
    val score: Float?,                      // ── nullable로 변경: 문서 아님일 때 null
    val reasons: List<String> = emptyList() // 위조 판단 근거 (정상이면 빈 리스트)
)

// ── 기존 데이터 클래스 (주석처리) ──────────────────────────────────
// data class StampInfo(
//     val error: Boolean,
//     val count: Int,
//     val boxes: List<StampBox>,
//     val score: Float
// )
//
// data class StampBox(
//     val x: Int,
//     val y: Int,
//     val width: Int,
//     val height: Int
// )
//
// data class KeywordInfo(
//     val error: Boolean,
//     val total_score: Float,
//     val details: List<Any>,
//     val is_document: Boolean = true
// )
//
// data class LayoutInfo(
//     val error: Boolean,
//     val score: Float
// )