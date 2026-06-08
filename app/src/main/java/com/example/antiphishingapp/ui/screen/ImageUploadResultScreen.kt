package com.example.antiphishingapp.ui.screen

import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.ui.text.font.FontWeight
import com.example.antiphishingapp.R
import com.example.antiphishingapp.feature.model.AnalysisResponse
import com.example.antiphishingapp.network.ApiClient
import com.example.antiphishingapp.theme.*
import com.example.antiphishingapp.feature.model.SuspiciousItem

// ── 기존 import (주석처리) ───────────────────────────────────────────
// import com.example.antiphishingapp.feature.model.StampBox

@Composable
fun ImageUploadResultScreen(
    navController: NavController,
    analysis: AnalysisResponse
) {
    val scrollState = rememberScrollState()

    // ── 기존 위험도 계산 (주석처리) ──────────────────────────────────
    // val forgeryScore = (analysis.final_risk * 100).toInt()

    // ── 새로운 위험도 계산 ────────────────────────────────────────────
    // score가 null인 경우 (document_detected=false 후 강제검사 등) 기본값 50 사용
    val forgeryScore = analysis.forgery.score?.let { score ->
        (99f - (score + 0.25f) * 196f).coerceIn(1f, 99f).toInt()
    } ?: 50

    val scoreColor = calculateScoreColor(forgeryScore)

    val fullImageUrl = ApiClient.AI_BASE_URL.removeSuffix("/") + analysis.url
    Log.d("IMAGE_URL", "이미지 URL: $fullImageUrl")  // ← 추가


    var showPopup by remember { mutableStateOf(false) }
    val painter = rememberAsyncImagePainter(fullImageUrl)
    val suspiciousItems = generateSuspiciousItems(analysis)

    Scaffold(containerColor = Primary100) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                /***********************
                 * 상단 위험도 표시
                 ***********************/
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 24.dp)
                ) {
                    Text(
                        text = "문서 위조 위험도",
                        style = MaterialTheme.typography.titleSmall,
                        color = Grayscale600,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "$forgeryScore%",
                            style = MaterialTheme.typography.displayMedium.copy(
                                fontFamily = Pretendard,
                                fontWeight = FontWeight.Bold,
                                color = scoreColor
                            ),
                            modifier = Modifier.padding(end = 16.dp)
                        )

                        Column {
                            Text(
                                text = when {
                                    forgeryScore >= 70 -> "위조 문서일 확률이 높습니다."
                                    forgeryScore >= 45 -> "위조 문서일 가능성이 있습니다."
                                    else -> "위조 문서일 가능성이 낮습니다."
                                },
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = Grayscale900
                                )
                            )

                            Text(
                                text = when {
                                    forgeryScore >= 70 -> "보이스피싱 등 범죄 목적으로 위조된 문서일 가능성이 높습니다."
                                    forgeryScore >= 45 -> "주의 깊게 확인해주세요."
                                    else -> "주요 위조 의심 징후가 탐지되지 않았습니다."
                                },
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = Grayscale900
                                ),
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }

                /***********************
                 * 이미지 미리보기 영역
                 ***********************/
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(scrollState)
                        .padding(horizontal = 24.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(320.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Grayscale50)
                    ) {
                        AsyncImage(
                            model = fullImageUrl,
                            contentDescription = "Uploaded Image",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )

                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 20.dp)
                        ) {
                            InteractiveResultButton(
                                text = "이미지 탐지 결과 살펴보기",
                                onClick = { showPopup = true }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    /***********************
                     * 위조 의심 항목
                     ***********************/
                    Text(
                        text = "위조 의심 항목",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Grayscale600,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    SuspiciousItemsBox(items = suspiciousItems)

                    Spacer(modifier = Modifier.height(32.dp))
                }

                /***********************
                 * 다른 문서로 다시 분석
                 ***********************/
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "다른 문서로 다시 시도해볼까요?",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Grayscale500,
                            fontWeight = FontWeight.Medium
                        ),
                        modifier = Modifier.clickable {
                            navController.popBackStack()
                        }
                    )
                }
            }

            /*****************************
             * 전체 화면 팝업 (이미지만)
             *****************************/
            if (showPopup) {
                FullscreenImageOverlay(
                    imageUrl = fullImageUrl,
                    onClose = { showPopup = false }
                )
            }
        }
    }
}

// ── 기존 FullscreenImageOverlay (stamp boxes 포함, 주석처리) ──────────
// @Composable
// fun FullscreenImageOverlay(
//     imageUrl: String,
//     fallbackPainter: Painter,
//     boxes: List<StampBox>,
//     onClose: () -> Unit
// ) { ... }

// ── 새로운 FullscreenImageOverlay (이미지만 표시) ─────────────────────
@Composable
fun FullscreenImageOverlay(
    imageUrl: String,
    onClose: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f)),
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = imageUrl,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )

        Icon(
            imageVector = Icons.Default.Close,
            contentDescription = "닫기",
            tint = Color.White,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(24.dp)
                .size(32.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable { onClose() }
        )
    }
}

@Composable
fun calculateScoreColor(score: Int): Color {
    val ratio = score.coerceIn(0, 100) / 100f
    return when {
        ratio <= 0.5f -> {
            val t = ratio / 0.5f
            lerp(GradientB_Start, GradientB_Mid, t)
        }
        else -> {
            val t = (ratio - 0.5f) / 0.5f
            lerp(GradientB_Mid, GradientB_End, t)
        }
    }
}

@Composable
fun InteractiveResultButton(text: String, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val isPressed by interaction.collectIsPressedAsState()
    val isHover by interaction.collectIsHoveredAsState()
    val active = isPressed || isHover

    Button(
        onClick = onClick,
        interactionSource = interaction,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (active) Primary900 else Primary300,
            contentColor = if (active) Primary100 else Primary900
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth(0.9f)
            .height(52.dp)
    ) {
        Icon(
            painter = painterResource(id = R.drawable.mag),
            contentDescription = "Search Icon",
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(text, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun SuspiciousItemsBox(items: List<SuspiciousItem>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Grayscale50)
            .padding(vertical = 16.dp)
    ) {
        items.forEachIndexed { index, item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, end = 11.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = null,
                    tint = Color(0xFFF13842),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Grayscale900,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 20.sp
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .padding(top = 1.dp)
                )
            }
            if (index < items.size - 1) {
                Spacer(Modifier.height(20.dp))
            }
        }
    }
}

// ── 기존 generateSuspiciousItems (주석처리) ───────────────────────────
// fun generateSuspiciousItems(analysis: AnalysisResponse): List<SuspiciousItem> {
//     val list = mutableListOf<SuspiciousItem>()
//     if (analysis.stamp.count > 0) list.add(SuspiciousItem("직인 탐지 영역 ${analysis.stamp.count}개 발견됨."))
//     if (!analysis.keyword.error && analysis.keyword.total_score > 0)
//         list.add(SuspiciousItem("위험 키워드 감지: 총 점수 ${analysis.keyword.total_score}"))
//     if (!analysis.layout.error && analysis.layout.score > 0.3f)
//         list.add(SuspiciousItem("문서 레이아웃이 비정상적으로 감지되었습니다."))
//     if (list.isEmpty()) list.add(SuspiciousItem("위조 의심 항목이 없습니다."))
//     return list
// }

// ── 새로운 generateSuspiciousItems ───────────────────────────────────
fun generateSuspiciousItems(analysis: AnalysisResponse): List<SuspiciousItem> {
    val list = mutableListOf<SuspiciousItem>()

    // ── 크롭 발생 여부 로그 ───────────────────────────────────────────
    Log.d("FORGERY", "✂️ 문서 영역 크롭 발생: ${analysis.forgery.was_cropped}")

    // ── is_forged가 null인 경우 (문서 아님) 처리 ─────────────────────
    if (analysis.forgery.is_forged == null) {
        list.add(SuspiciousItem("문서로 판별되지 않은 이미지입니다."))
        return list
    }

    if (analysis.forgery.is_forged == true) {
        // 로그캣에 reasons 출력
        Log.d("FORGERY", "🔍 위조 판단 근거 (${analysis.forgery.reasons.size}개):")
        analysis.forgery.reasons.forEachIndexed { index, reason ->
            Log.d("FORGERY", "  ${index + 1}. $reason")
        }

        // 화면 표시
        if (analysis.forgery.reasons.isNotEmpty()) {
            analysis.forgery.reasons.forEach { reason ->
                list.add(SuspiciousItem(reason))
            }
        } else {
            list.add(SuspiciousItem("AI 분석 결과 위조 문서로 판정되었습니다. (점수: ${analysis.forgery.score})"))
        }
    } else {
        Log.d("FORGERY", "✅ 정상 문서로 판정되었습니다. (점수: ${analysis.forgery.score})")
        list.add(SuspiciousItem("위조 의심 항목이 없습니다."))
    }

    return list
}