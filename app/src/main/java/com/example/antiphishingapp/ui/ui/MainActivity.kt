package com.example.antiphishingapp.ui

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.antiphishingapp.ui.theme.AntiPhishingAppTheme
import com.example.antiphishingapp.feature.StampDetector
import com.example.antiphishingapp.feature.OcrService
import com.example.antiphishingapp.feature.model.DetectionResult
import com.example.antiphishingapp.network.RetrofitClient
import com.example.antiphishingapp.network.UploadResponse
import com.example.antiphishingapp.utils.bitmapToMultipart
import okhttp3.MultipartBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import org.opencv.android.OpenCVLoader
import org.opencv.core.Rect
import java.io.InputStream

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (!isGranted) {
                Toast.makeText(this, "이미지 접근 권한이 필요합니다", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // OpenCV 초기화
        if (!OpenCVLoader.initDebug()) {
            Log.e("OpenCV", "OpenCV 초기화 실패")
        }

        // 권한 요청
        checkImagePermission()

        setContent {
            AntiPhishingAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PhishingDetectScreen()
                }
            }
        }
    }

    private fun checkImagePermission() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        when {
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED -> {
                Log.d("PERMISSION", "이미지 접근 권한 허용됨")
            }
            else -> {
                requestPermissionLauncher.launch(permission)
            }
        }
    }
}

@Composable
fun PhishingDetectScreen() {
    var detectionResult by remember { mutableStateOf<DetectionResult?>(null) }
    var ocrResult by remember { mutableStateOf<String>("") }
    val context = LocalContext.current

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val inputStream: InputStream? = context.contentResolver.openInputStream(it)
                if (inputStream == null) {
                    Toast.makeText(context, "이미지를 불러올 수 없습니다", Toast.LENGTH_SHORT).show()
                    return@let
                }

                val bitmap = BitmapFactory.decodeStream(inputStream)

                // (1) 서버 업로드
                val part: MultipartBody.Part = bitmapToMultipart(bitmap)
                RetrofitClient.instance.uploadImage(part).enqueue(object : Callback<UploadResponse> {
                    override fun onResponse(call: Call<UploadResponse>, response: Response<UploadResponse>) {
                        if (response.isSuccessful) {
                            val body = response.body()
                            Log.d("UPLOAD", "서버 업로드 성공: ${body?.url}")
                            Toast.makeText(context, "업로드 성공: ${body?.url}", Toast.LENGTH_SHORT).show()
                        } else {
                            Log.e("UPLOAD", "서버 업로드 실패: ${response.code()}")
                            Toast.makeText(context, "업로드 실패: ${response.code()}", Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onFailure(call: Call<UploadResponse>, t: Throwable) {
                        Log.e("UPLOAD", "서버 업로드 에러: ${t.message}")
                        Toast.makeText(context, "업로드 에러: ${t.message}", Toast.LENGTH_SHORT).show()
                    }
                })

                // (2) 로컬 스탬프 탐지
                detectionResult = StampDetector.findStampRoi(bitmap, context)

                // (3) 로컬 OCR
                OcrService.performOcr(bitmap) { result ->
                    ocrResult = result
                    Log.d("OCR_RESULT", result)
                }
            } catch (e: SecurityException) {
                Log.e("GALLERY", "권한 문제: ${e.message}")
                Toast.makeText(context, "권한 문제: ${e.message}", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.e("GALLERY", "이미지 처리 중 오류: ${e.message}")
                Toast.makeText(context, "이미지 처리 오류: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (detectionResult != null) {
            AnalyzedImageWithBoxes(
                bitmap = detectionResult!!.bitmap,
                boxes = detectionResult!!.boxes,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text("아래 버튼을 눌러 이미지를 선택하세요.")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (ocrResult.isNotEmpty()) {
            Text(
                text = "OCR 결과: $ocrResult",
                modifier = Modifier.padding(8.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = { galleryLauncher.launch("image/*") }) {
            Text("이미지 불러오기")
        }
    }
}

@Composable
fun AnalyzedImageWithBoxes(
    bitmap: Bitmap,
    boxes: List<Rect>,
    modifier: Modifier = Modifier
) {
    val originalWidth = bitmap.width.toFloat()
    val originalHeight = bitmap.height.toFloat()

    Box(
        modifier = modifier.aspectRatio(originalWidth / originalHeight)
    ) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = null,
            modifier = Modifier.matchParentSize()
        )

        Canvas(modifier = Modifier.matchParentSize()) {
            val scaleX = size.width / originalWidth
            val scaleY = size.height / originalHeight

            boxes.forEach { rect ->
                drawRect(
                    color = Color.Yellow,
                    topLeft = Offset(rect.x * scaleX, rect.y * scaleY),
                    size = Size(rect.width * scaleX, rect.height * scaleY),
                    style = Stroke(width = 5f)
                )
            }
        }
    }
}
