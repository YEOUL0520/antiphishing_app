package com.example.antiphishingapp.ui.screen

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
import com.example.antiphishingapp.feature.model.StampBox
import com.example.antiphishingapp.network.ApiClient
import com.example.antiphishingapp.theme.*
import com.example.antiphishingapp.feature.model.SuspiciousItem

// ✅ 메인 화면 – 문서 분석 결과 (FileUploadScreen에서 문서만 이쪽으로 넘겨준다고 가정)
@Composable
fun ImageUploadResultScreen(
    navController: NavController,
    analysis: AnalysisResponse
) {
    val scrollState = rememberScrollState()

    val forgeryScore = (analysis.final_risk * 100).toInt()
    val scoreColor = calculateScoreColor(forgeryScore)

    val fullImageUrl = ApiClient.BASE_URL.removeSuffix("/") + analysis.url

    // 팝업 모드 ON/OFF
    var showPopup by remember { mutableStateOf(false) }

    // Painter 공유 (팝업에서도 같은 이미지 사용)
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
                 * 2) 이미지 미리보기 영역
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
                        // 축소된 원본 이미지 표시
                        AsyncImage(
                            model = fullImageUrl,
                            contentDescription = "Uploaded Image",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )

                        // 버튼 (팝업 띄우기)
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
                     * 3) 위조 의심 항목
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
                 * 4) 다른 문서로 다시 분석
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

            /*****************************************************
             * 5) 전체 화면 팝업 레이어 (원본 이미지 + 박스)
             *****************************************************/
            if (showPopup) {
                FullscreenImageOverlay(
                    imageUrl = fullImageUrl,
                    fallbackPainter = painter,
                    boxes = analysis.stamp.boxes,
                    onClose = { showPopup = false }
                )
            }
        }
    }
}

// 전체 화면 오버레이 팝업
@Composable
fun FullscreenImageOverlay(
    imageUrl: String,
    fallbackPainter: Painter,
    boxes: List<StampBox>,
    onClose: () -> Unit
) {
    // Coil에서 확정된 "실제 이미지(원본) 크기" 저장
    var originalSize by remember { mutableStateOf<IntSize?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f)),
        contentAlignment = Alignment.Center
    ) {

        // 중앙 이미지 + 박스 오버레이
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            /**
             * onSuccess로 drawable intrinsicWidth/Height 확보 (coil-compose:2.4.0)
             * - 서버 bbox가 "원본 픽셀" 기준이므로, 이 크기를 overlay 기준으로 사용
             */
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
                onSuccess = { success ->
                    val d = success.result.drawable
                    val w = d.intrinsicWidth
                    val h = d.intrinsicHeight
                    if (w > 0 && h > 0) {
                        originalSize = IntSize(w, h)
                    }
                }
            )

            // 박스도 같은 영역 위에 그리기 (Fit + offset 보정)
            StampBoxOverlay(
                originalSize = originalSize,
                fallbackPainter = fallbackPainter,
                boxes = boxes,
                modifier = Modifier.fillMaxSize()
            )
        }

        // 닫기 버튼
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

/**
 * 박스 오버레이
 * 1) Fit 스케일 + offset 보정 적용
 * 2) "원본 이미지 크기"는 Coil onSuccess로 받은 originalSize를 우선 사용
 *    - originalSize가 아직 null이면 fallbackPainter.intrinsicSize로 임시 처리
 */
@Composable
fun StampBoxOverlay(
    originalSize: IntSize?,
    fallbackPainter: Painter,
    boxes: List<StampBox>,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier) {
        val renderW = constraints.maxWidth.toFloat()
        val renderH = constraints.maxHeight.toFloat()
        if (renderW <= 0f || renderH <= 0f) return@BoxWithConstraints

        val (imgW, imgH) = if (originalSize != null && originalSize.width > 0 && originalSize.height > 0) {
            originalSize.width.toFloat() to originalSize.height.toFloat()
        } else {
            // fallback (정확하지 않을 수 있지만, originalSize가 잡히면 자동으로 재컴포즈되어 교정됨)
            val w = fallbackPainter.intrinsicSize.width
            val h = fallbackPainter.intrinsicSize.height
            if (w <= 0f || h <= 0f) return@BoxWithConstraints
            w to h
        }

        // ContentScale.Fit 수학: min 스케일 + 중앙 offset(레터박스)
        val scale = minOf(renderW / imgW, renderH / imgH)
        val drawW = imgW * scale
        val drawH = imgH * scale
        val offsetX = (renderW - drawW) / 2f
        val offsetY = (renderH - drawH) / 2f

        Canvas(modifier = Modifier.fillMaxSize()) {
            boxes.forEach { b ->
                drawRect(
                    color = Color.Red,
                    topLeft = Offset(
                        x = offsetX + b.x * scale,
                        y = offsetY + b.y * scale
                    ),
                    size = Size(
                        width = b.width * scale,
                        height = b.height * scale
                    ),
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )
            }
        }
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

fun generateSuspiciousItems(analysis: AnalysisResponse): List<SuspiciousItem> {
    val list = mutableListOf<SuspiciousItem>()
    if (analysis.stamp.count > 0) list.add(SuspiciousItem("직인 탐지 영역 ${analysis.stamp.count}개 발견됨."))
    if (!analysis.keyword.error && analysis.keyword.total_score > 0)
        list.add(SuspiciousItem("위험 키워드 감지: 총 점수 ${analysis.keyword.total_score}"))
    if (!analysis.layout.error && analysis.layout.score > 0.3f)
        list.add(SuspiciousItem("문서 레이아웃이 비정상적으로 감지되었습니다."))
    if (list.isEmpty()) list.add(SuspiciousItem("위조 의심 항목이 없습니다."))
    return list
}