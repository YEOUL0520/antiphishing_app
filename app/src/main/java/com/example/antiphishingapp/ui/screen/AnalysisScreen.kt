package com.example.antiphishingapp.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.antiphishingapp.feature.model.AnalysisResponse

@Composable
fun AnalysisScreen(
    result: AnalysisResponse,
    onBackToMain: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "🔍 서버 분석 결과",
            style = MaterialTheme.typography.headlineSmall,
            color = Color(0xFF1E88E5)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // ── 기존 결과 표시 (주석처리) ─────────────────────────────────
        // Text("📄 파일명: ${result.filename}")
        // Text("📦 스탬프 개수: ${result.stamp.count}")
        // Text("🧩 스탬프 점수: ${result.stamp.score}")
        // Text("📐 레이아웃 점수: ${result.layout.score}")
        // Text("⚠️ 최종 위험도: ${(result.final_risk * 100).toInt()}%")

        // ── 새로운 결과 표시 ──────────────────────────────────────────
        Text("📄 파일명: ${result.filename}")
        Text("🔎 판정 결과: ${result.forgery.result}")
        Text("📊 AI 점수: ${result.forgery.score}")
        Text(
            text = if (result.forgery.is_forged) "🚨 위조 의심 문서입니다."
            else "✅ 정상 문서입니다."
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = { onBackToMain() },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2))
        ) {
            Text("다시 분석하기", color = Color.White)
        }
    }
}