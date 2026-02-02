package com.example.antiphishingapp.feature.model

data class AnalysisResponse(
    val filename: String,
    val url: String,
    val stamp: StampInfo,
    val keyword: KeywordInfo,
    val layout: LayoutInfo,
    val final_risk: Float
)

data class StampInfo(
    val error: Boolean,
    val count: Int,
    val boxes: List<StampBox>,
    val score: Float
)

data class StampBox(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int
)

data class KeywordInfo(
    val error: Boolean,
    val total_score: Float,
    val details: List<Any>,
    val is_document: Boolean = true
)

data class LayoutInfo(
    val error: Boolean,
    val score: Float
)
