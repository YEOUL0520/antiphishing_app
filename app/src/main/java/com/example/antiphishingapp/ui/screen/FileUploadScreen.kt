package com.example.antiphishingapp.ui.screen

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.antiphishingapp.R
import com.example.antiphishingapp.feature.model.AnalysisResponse
import com.example.antiphishingapp.feature.model.VoiceUiResult
import com.example.antiphishingapp.feature.viewmodel.AnalysisViewModel
import com.example.antiphishingapp.feature.viewmodel.AuthViewModel
import com.example.antiphishingapp.feature.viewmodel.VoiceAnalysisViewModel
import com.example.antiphishingapp.theme.*
import com.example.antiphishingapp.ui.components.FailureDialogCard
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
//import org.tensorflow.lite.Interpreter
import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import kotlin.math.abs

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

    val loading by analysisViewModel.loading.observeAsState(false)
    val result by analysisViewModel.result.observeAsState()

    val voiceLoading by voiceAnalysisViewModel.loading.observeAsState(false)
    val voiceResult by voiceAnalysisViewModel.result.observeAsState()
    val voiceError by voiceAnalysisViewModel.error.observeAsState()

    val context = LocalContext.current
    var voiceErrorDialog by remember { mutableStateOf<String?>(null) }

    var showNotDocOverlay by remember { mutableStateOf(false) }

    // ── 기존 방식 선택 다이얼로그 관련 변수 (주석처리) ────────────────
    // var showMethodSelectionDialog by remember { mutableStateOf(false) }
    // var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var serverStartTime by remember { mutableStateOf(0L) }

    // 음성 업로드
    val pickAudioLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            val file = uriToTempFile(context, uri)
            voiceAnalysisViewModel.analyzeVoice(file)
        }
    }

    LaunchedEffect(voiceError) {
        voiceError?.let { msg ->
            voiceErrorDialog = msg
            voiceAnalysisViewModel.clearError()
        }
    }

    // ── 이미지 업로드: 선택 즉시 바로 서버로 전송 ────────────────────
    val pickImageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            serverStartTime = System.currentTimeMillis()
            Log.d("AI_TEST", "🌐 서버로 이미지 전송 시작...")
            val multipart = uriToMultipart("file", uri, context)
            analysisViewModel.analyzeDocument(multipart)
        }
    }

    // ── 기존 이미지 업로드 (선택 메뉴 띄우기, 주석처리) ──────────────
    // val pickImageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
    //     if (uri != null) {
    //         selectedImageUri = uri
    //         showMethodSelectionDialog = true
    //     }
    // }

    // ---- Image result (문서 여부 판별 → 분기) ----
    LaunchedEffect(result) {
        result?.let { analysis ->
            if (serverStartTime > 0L) {
                val endTime = System.currentTimeMillis()
                val duration = endTime - serverStartTime
                Log.d("AI_TEST", "🌐 분석 완료! 소요 시간: ${duration}ms")
                serverStartTime = 0L
            }

            // ── 기존 is_document 판단 로직 (주석처리) ─────────────────
            // val isDocument = !analysis.keyword.error && analysis.keyword.is_document
            // if (isDocument) {
            //     onUploadSuccess(analysis)
            // } else {
            //     showNotDocOverlay = true
            // }

            // ── 새로운 위조 판단 로직 ──────────────────────────────────
            onUploadSuccess(analysis)

            analysisViewModel.resetResult()
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = Primary100) {
        Box(modifier = Modifier.fillMaxSize()) {

            // 메인 화면
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp)
                    // ── 기존: showMethodSelectionDialog 조건 포함 (주석처리)
                    // .then(if (showNotDocOverlay || showMethodSelectionDialog) Modifier.blur(10.dp) else Modifier)
                    .then(if (showNotDocOverlay) Modifier.blur(10.dp) else Modifier)
            ) {
                Spacer(modifier = Modifier.height(32.dp))
                TopBar(userName = userName)
                Spacer(modifier = Modifier.height(62.dp))
                FileUploadHeader()
                Spacer(modifier = Modifier.height(32.dp))

                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
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

            // 로딩 오버레이
            if (loading || voiceLoading) {
                Box(
                    Modifier.fillMaxSize().background(Grayscale300.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator(color = Primary900) }
            }

            voiceErrorDialog?.let { errMsg ->
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Grayscale300.copy(alpha = 0.35f)),
                    contentAlignment = Alignment.Center
                ) {
                    FailureDialogCard(
                        title = "음성 분석에 실패했습니다",
                        message = errMsg,
                        confirmText = "확인",
                        confirmEnabled = true,
                        onConfirm = { voiceErrorDialog = null }
                    )
                }
            }

            // 실패 오버레이
            BackHandler(enabled = showNotDocOverlay) { showNotDocOverlay = false }
            if (showNotDocOverlay) {
                Box(
                    modifier = Modifier.fillMaxSize().background(Grayscale300.copy(alpha = 0.25f)).clickable { },
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

            // ── 기존 방식 선택 다이얼로그 (주석처리) ─────────────────────
            // if (showMethodSelectionDialog && selectedImageUri != null) {
            //     AlertDialog(
            //         onDismissRequest = { showMethodSelectionDialog = false },
            //         title = { Text("검사 방식 선택") },
            //         text = { Text("어떤 방식으로 위조 여부를 검사할까요?\n(결과는 Logcat에서 확인하세요)") },
            //         confirmButton = {
            //             TextButton(onClick = {
            //                 showMethodSelectionDialog = false
            //                 serverStartTime = System.currentTimeMillis()
            //                 Log.d("AI_TEST", "🌐 [기존 방식] 서버로 이미지 전송 시작...")
            //                 val multipart = uriToMultipart("file", selectedImageUri!!, context)
            //                 analysisViewModel.analyzeDocument(multipart)
            //             }) {
            //                 Text("기존 방식 (서버)")
            //             }
            //         },
            //         dismissButton = {
            //             TextButton(onClick = {
            //                 showMethodSelectionDialog = false
            //                 Log.d("AI_TEST", "🤖 [새 방식] 폰 내부 AI 분석 시작...")
            //                 runOnDeviceAiMethod(context, selectedImageUri!!) { score ->
            //                     if (score > 0.0028) {
            //                         Log.d("AI_TEST", "🚨 위조 의심! 제미나이에게 2차 정밀 분석을 요청합니다.")
            //                         val bitmap = uriToBitmap(context, selectedImageUri!!)
            //                         analysisViewModel.analyzeWithGemini(bitmap, score)
            //                     } else {
            //                         Log.d("AI_TEST", "✅ 정상 문서입니다. 제미나이를 호출하지 않습니다.")
            //                     }
            //                 }
            //             }) {
            //                 Text("새 방식 (AI 모델)")
            //             }
            //         }
            //     )
            // }
        }
    }
}

// ======================================================================
// 🤖 새 방식 (AI 모델) 판별 도우미 함수들 (주석처리)
// ======================================================================

// fun runOnDeviceAiMethod(context: Context, uri: Uri, onResult: (Double) -> Unit) {
//     val startTime = System.currentTimeMillis()
//     try {
//         val tfliteModel = loadModelFile(context, "model_float32_v01.tflite")
//         val interpreter = Interpreter(tfliteModel)
//         val bitmap = uriToBitmap(context, uri)
//         val resizedBitmap = Bitmap.createScaledBitmap(bitmap, 128, 128, true)
//         val inputBuffer = bitmapToByteBuffer(resizedBitmap)
//         val outputBuffer = ByteBuffer.allocateDirect(4 * 1 * 3 * 128 * 128)
//         outputBuffer.order(ByteOrder.nativeOrder())
//         interpreter.run(inputBuffer, outputBuffer)
//         inputBuffer.rewind()
//         outputBuffer.rewind()
//         var sumError = 0.0
//         val totalPixels = 128 * 128 * 3
//         for (i in 0 until totalPixels) {
//             val originalPixel = inputBuffer.float
//             val aiPixel = outputBuffer.float
//             val diff = (originalPixel - aiPixel).toDouble()
//             sumError += diff * diff
//         }
//         val score = sumError / totalPixels
//         val endTime = System.currentTimeMillis()
//         val duration = endTime - startTime
//         Log.d("AI_TEST", "================ [1차 AI 검사 결과] ================")
//         Log.d("AI_TEST", "⏱ 소요 시간: ${duration}ms")
//         Log.d("AI_TEST", "📊 오차 점수: $score")
//         if (score > 0.011) {
//             Log.d("AI_TEST", "🚨 판정: 위조 의심 문서입니다! (제미나이 2차 검사로 넘어갑니다)")
//         } else {
//             Log.d("AI_TEST", "✅ 판정: 정상 문서입니다. (여기서 검사를 종료합니다)")
//         }
//         Log.d("AI_TEST", "=====================================================")
//         onResult(score)
//         interpreter.close()
//     } catch (e: Exception) {
//         Log.e("AI_TEST", "❌ AI 실행 중 오류 발생: ${e.message}")
//     }
// }

// fun loadModelFile(context: Context, modelName: String): ByteBuffer {
//     val assetFileDescriptor = context.assets.openFd(modelName)
//     val fileInputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
//     val fileChannel = fileInputStream.channel
//     val startOffset = assetFileDescriptor.startOffset
//     val declaredLength = assetFileDescriptor.declaredLength
//     return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
// }

fun uriToBitmap(context: Context, uri: Uri): Bitmap {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        val source = ImageDecoder.createSource(context.contentResolver, uri)
        ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            decoder.isMutableRequired = true
        }
    } else {
        @Suppress("DEPRECATION")
        MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
    }
}

fun bitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
    val byteBuffer = ByteBuffer.allocateDirect(4 * 1 * 3 * 128 * 128)
    byteBuffer.order(ByteOrder.nativeOrder())
    val intValues = IntArray(128 * 128)
    bitmap.getPixels(intValues, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
    var pixel = 0
    for (i in 0 until 128) {
        for (j in 0 until 128) {
            val valPixel = intValues[pixel++]
            byteBuffer.putFloat(((valPixel shr 16) and 0xFF) / 255.0f)
            byteBuffer.putFloat(((valPixel shr 8) and 0xFF) / 255.0f)
            byteBuffer.putFloat((valPixel and 0xFF) / 255.0f)
        }
    }
    return byteBuffer
}

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

//@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
//@Composable
//fun PreviewFileUploadHeader() {
//    FileUploadHeader()
//}
//
//@Preview(showBackground = true, backgroundColor = 0xFFF2F4F6)
//@Composable
//fun PreviewActionCard() {
//    ActionCard(
//        title = "이미지 업로드",
//        description = "의심되는 문서 스캔 이미지를 첨부해\n위험도 확인이 가능합니다.",
//        iconRes = R.drawable.image_upload,
//        onClick = {}
//    )
//}
//
//@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
//@Composable
//fun PreviewTopBar() {
//    TopBar(userName = "Shin")
//}