package com.example.antiphishingapp.ui.screen

import android.content.Context
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.antiphishingapp.R
import com.example.antiphishingapp.feature.model.AnalysisResponse
import com.example.antiphishingapp.feature.model.VoiceUiResult
import com.example.antiphishingapp.feature.viewmodel.AnalysisViewModel
import com.example.antiphishingapp.feature.viewmodel.AuthViewModel
import com.example.antiphishingapp.feature.viewmodel.VoiceAnalysisViewModel
import com.example.antiphishingapp.theme.AppTypography
import com.example.antiphishingapp.theme.Grayscale300
import com.example.antiphishingapp.theme.Grayscale600
import com.example.antiphishingapp.theme.Grayscale700
import com.example.antiphishingapp.theme.Grayscale800
import com.example.antiphishingapp.theme.Grayscale900
import com.example.antiphishingapp.theme.NPSFont
import com.example.antiphishingapp.theme.Primary100
import com.example.antiphishingapp.theme.Primary300
import com.example.antiphishingapp.theme.Primary900
import com.example.antiphishingapp.ui.components.FailureDialogCard
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

@Composable
fun FileUploadScreen(
    navController: NavController,
    authViewModel: AuthViewModel,
    analysisViewModel: AnalysisViewModel,
    voiceAnalysisViewModel: VoiceAnalysisViewModel,
    onUploadSuccess: (AnalysisResponse) -> Unit,
    onVoiceUploadSuccess: (VoiceUiResult) -> Unit
) {
    val userState by authViewModel.user.collectAsState()
    val userName = userState?.fullName ?: "사용자"

    // ViewModel state
    val loading by analysisViewModel.loading.observeAsState(false)
    val result by analysisViewModel.result.observeAsState()

    val voiceLoading by voiceAnalysisViewModel.loading.observeAsState(false)
    val voiceResult by voiceAnalysisViewModel.result.observeAsState()

    val context = LocalContext.current

    // 문서 아닌 이미지일 때 오버레이 표시
    var showNotDocOverlay by remember { mutableStateOf(false) }

    // ---- Launchers ----
    val pickAudioLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                val file = uriToTempFile(context, uri)
                voiceAnalysisViewModel.analyzeVoice(file)
            }
        }

    val pickImageLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                val multipart = uriToMultipart("file", uri, context)
                analysisViewModel.analyzeDocument(multipart)
            }
        }

    // ---- Voice result ----
    LaunchedEffect(voiceResult) {
        voiceResult?.let { r ->
            onVoiceUploadSuccess(r)
            voiceAnalysisViewModel.resetResult()
        }
    }

    // ---- Image result (문서 여부 판별 → 분기) ----
    LaunchedEffect(result) {
        result?.let { analysis ->
            val isDocument = !analysis.keyword.error && analysis.keyword.is_document

            if (isDocument) {
                // 문서면 결과 화면으로 넘김
                onUploadSuccess(analysis)
            } else {
                // 문서 아니면 FileUploadScreen에서 오버레이로 처리
                showNotDocOverlay = true
            }

            // 다음 업로드 대비 초기화 (중복 트리거 방지)
            analysisViewModel.resetResult()
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Primary100
    ) {
        Box(modifier = Modifier.fillMaxSize()) {

            // 기존 화면 (오버레이 뜨면 blur)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp)
                    .then(if (showNotDocOverlay) Modifier.blur(10.dp) else Modifier)
            ) {
                Spacer(modifier = Modifier.height(32.dp))

                TopBar(userName = userName)
                Spacer(modifier = Modifier.height(62.dp))

                FileUploadHeader()
                Spacer(modifier = Modifier.height(32.dp))

                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    ActionCard(
                        title = "이미지 업로드",
                        description = "의심되는 문서 스캔 이미지를 첨부해\n위험도 확인이 가능합니다.",
                        iconRes = R.drawable.image_upload,
                        onClick = { pickImageLauncher.launch("image/*") }
                    )

                    Spacer(modifier = Modifier.height(25.dp))

                    ActionCard(
                        title = "음성 업로드",
                        description = "의심되는 통화 녹음 파일을 첨부해\n위험도 확인이 가능합니다.",
                        iconRes = R.drawable.voice_upload,
                        onClick = { pickAudioLauncher.launch("audio/*") }
                    )

                    Spacer(modifier = Modifier.weight(1f))
                    HelpSection(modifier = Modifier.padding(vertical = 64.dp))
                }
            }

            // 로딩 오버레이 (이미지)
            if (loading) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Grayscale300.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Primary900)
                }
            }

            // 로딩 오버레이 (음성)
            if (voiceLoading) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Grayscale300.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Primary900)
                }
            }

            // 문서 인식 실패 시 FailureDialogCard 오버레이
            // Back 버튼 입력시 오버레이를 제거 (FileUploadScreen으로 돌아가도록)
            BackHandler(enabled = showNotDocOverlay) {
                showNotDocOverlay = false
            }

            if (showNotDocOverlay) {
                val interaction = remember { MutableInteractionSource() }

                // 배경 dim + 터치 흡수
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Grayscale300.copy(alpha = 0.25f))
                        .clickable(
                            interactionSource = interaction,
                            indication = null
                        ) { },
                    contentAlignment = Alignment.Center
                ) {
                    FailureDialogCard(
                        title = "문서로 인식되지 않았습니다",
                        message = "글자를 찾지 못했습니다. 다른 사진(문서)을 선택해 주세요.",
                        confirmText = "다시 선택하기",
                        confirmEnabled = true,
                        onConfirm = {
                            showNotDocOverlay = false
                            pickImageLauncher.launch("image/*")
                        }
                    )
                }
            }

        }
    }
}

/**
 * URI → MultipartBody.Part 변환
 */
fun uriToMultipart(field: String, uri: Uri, context: Context): MultipartBody.Part {
    val inputStream = context.contentResolver.openInputStream(uri)!!
    val bytes = inputStream.readBytes()
    inputStream.close()

    val requestBody = bytes.toRequestBody("image/*".toMediaType())
    return MultipartBody.Part.createFormData(field, "upload.jpg", requestBody)
}

fun uriToTempFile(context: Context, uri: Uri): File {
    val inputStream = context.contentResolver.openInputStream(uri)
        ?: throw IllegalArgumentException("Uri InputStream is null")

    val tempFile = File.createTempFile("voice_upload_", ".tmp", context.cacheDir)

    inputStream.use { input ->
        tempFile.outputStream().use { output ->
            input.copyTo(output)
        }
    }
    return tempFile
}

@Composable
private fun TopBar(userName: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.clickable { },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Grayscale300)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = userName,
                style = AppTypography.titleMedium,
                color = Grayscale800
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        IconButton(onClick = { }) {
            Icon(
                imageVector = Icons.Default.Menu,
                contentDescription = "Menu",
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun FileUploadHeader() {
    Column {
        Text(
            text = "문서 위조 탐지 및 보이스피싱 탐지",
            style = AppTypography.bodyMedium,
            color = Grayscale900
        )
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(color = Primary900)) { append("파일") }
                append("을 ")
                withStyle(SpanStyle(color = Primary900)) { append("업로드") }
                append("하여")
            },
            style = AppTypography.headlineLarge.copy(
                fontFamily = NPSFont,
                fontWeight = FontWeight.Normal
            ),
            color = Grayscale900
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(color = Primary900)) { append("의심 정황") }
                append("을 확인해요.")
            },
            style = AppTypography.headlineLarge.copy(
                fontFamily = NPSFont,
                fontWeight = FontWeight.Normal
            ),
            color = Grayscale900
        )
    }
}

@Composable
private fun ActionCard(
    title: String,
    description: String,
    @DrawableRes iconRes: Int,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val cardColor = if (isPressed) Primary900 else Primary300
    val titleColor = if (isPressed) Primary300 else Primary900
    val descriptionColor = if (isPressed) Primary300 else Grayscale700

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = AppTypography.headlineLarge.copy(
                        fontFamily = NPSFont,
                        fontWeight = FontWeight.Bold
                    ),
                    color = titleColor
                )
                Spacer(Modifier.height(20.dp))
                Text(
                    text = description,
                    style = AppTypography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = descriptionColor,
                    lineHeight = 20.sp
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Image(
                painter = painterResource(id = iconRes),
                contentDescription = title,
                modifier = Modifier.size(100.dp)
            )
        }
    }
}

@Composable
private fun HelpSection(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "도움이 필요하신가요?",
            modifier = Modifier.clickable { },
            style = AppTypography.bodyMedium,
            color = Grayscale600
        )
    }
}
