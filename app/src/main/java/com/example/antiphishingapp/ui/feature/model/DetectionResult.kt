package com.example.antiphishingapp.feature.model

import android.graphics.Bitmap
import org.opencv.core.Rect

data class DetectionResult(
    val bitmap: Bitmap,          // 원본 이미지
    val boxes: List<Rect>,       // 직인 박스
    val ocrText: String? = null, // (옵션) OCR 텍스트
    val riskScore: Double? = null// (옵션) 위험도 0.0~1.0 등
)
