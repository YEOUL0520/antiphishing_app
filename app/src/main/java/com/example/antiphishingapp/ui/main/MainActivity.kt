package com.example.antiphishingapp.ui.main

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.antiphishingapp.ui.theme.AntiPhishingAppTheme
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.*
import org.json.JSONObject

// ---- 데이터 클래스: Bitmap + Rects 반환 ----
data class DetectionResult(
    val bitmap: Bitmap, // 원본 이미지
    val boxes: List<Rect> // 탐지된 객체 box 좌표
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // OpenCV 라이브러리 로드
        if (OpenCVLoader.initDebug()) {
            Log.d("MainActivity", "OpenCV initialized successfully")
        } else {
            Log.e("MainActivity", "OpenCV initialization failed")
        }

        // Compose UI (PhishingDetectScreen())실행
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
}

// --- UI ---
@Composable
fun PhishingDetectScreen() {
    var detectionResult by remember { mutableStateOf<DetectionResult?>(null) }
    val context = LocalContext.current

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent() // 갤러리에서 이미지 가져옴
    ) { uri: Uri? ->
        uri?.let {
            val inputStream = context.contentResolver.openInputStream(it)
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            detectionResult = findStampRoi(originalBitmap, context)
        }
    }
    // 화면 구조
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
                Text(text = "아래 버튼을 눌러 이미지를 선택하세요.")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = { galleryLauncher.launch("image/*") }) {
            Text(text = "이미지 불러오기")
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

// ---- 이미지 + 박스 표시 ----
@Composable
fun AnalyzedImageWithBoxes(
    bitmap: Bitmap,
    boxes: List<Rect>,
    modifier: Modifier = Modifier
) {
    val originalWidth = bitmap.width.toFloat()
    val originalHeight = bitmap.height.toFloat()

    Box(
        modifier = modifier
            .aspectRatio(originalWidth / originalHeight) // 원본 비율 유지
    ) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = null,
            modifier = Modifier.matchParentSize()
        )

        Canvas(modifier = Modifier.matchParentSize()) {
            val scaleX = size.width / originalWidth // 박스 좌표를 실제 화면 표시 크기로 보정
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

// --- Logic: 박스 좌표 추출 ---
private fun findStampRoi(inputBitmap: Bitmap, context: Context): DetectionResult {
    val srcMat = Mat()
    Utils.bitmapToMat(inputBitmap, srcMat) // 이미지를 Mat 형태로 변환

    val bgrMat = Mat()
    Imgproc.cvtColor(srcMat, bgrMat, Imgproc.COLOR_RGBA2BGR) // rgba->bgr

    val hsvMat = Mat()
    Imgproc.cvtColor(bgrMat, hsvMat, Imgproc.COLOR_BGR2HSV) // bgr->hsv

    // 탐지 색상 범위 설정
    val lowerRed1 = Scalar(0.0, 40.0, 50.0)
    val upperRed1 = Scalar(10.0, 255.0, 255.0)
    val lowerRed2 = Scalar(170.0, 40.0, 50.0)
    val upperRed2 = Scalar(180.0, 255.0, 255.0)

    val mask1 = Mat()
    val mask2 = Mat()
    Core.inRange(hsvMat, lowerRed1, upperRed1, mask1)
    Core.inRange(hsvMat, lowerRed2, upperRed2, mask2)

    val redMask = Mat()
    Core.bitwise_or(mask1, mask2, redMask)

    // 모폴로지 연산 (빈 픽셀 채움, 노이즈 제거 통해 해당 영역만 깔끔하게 남김)
    val kernel = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, Size(5.0, 5.0))
    Imgproc.morphologyEx(redMask, redMask, Imgproc.MORPH_CLOSE, kernel)
    Imgproc.morphologyEx(redMask, redMask, Imgproc.MORPH_OPEN, kernel)

    // 컨투어 탐색
    val contours = ArrayList<MatOfPoint>()
    val hierarchy = Mat()
    Imgproc.findContours(redMask, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE)

    // 외곽선(contour) 감싸는 최소 직사각형 좌표 추출
    val rects = mutableListOf<Rect>()
    if (contours.isNotEmpty()) {
        for (contour in contours) {
            if (Imgproc.contourArea(contour) > 1000) { // 1000보다 작은 크기는 노이즈로 간주
                val rect = Imgproc.boundingRect(contour)
                rects.add(rect)
            }
        }
    }

    if (rects.isEmpty()) {
        Toast.makeText(context, "직인 영역을 찾지 못했습니다.", Toast.LENGTH_SHORT).show()
    }

    return DetectionResult(inputBitmap, rects)
}

// --- Logic: Naver CLOVA OCR API 호출 ---
private fun performOcr(bitmap: Bitmap, onResult: (String) -> Unit) {

    val apiUrl = "https://fwymjktetd.apigw.ntruss.com/custom/v1/45162/f06f44fc9667be94a98feed9824ad4f1bb0c7a35bf9e32132fc012be76435739/general"
    val secretKey = "S0daUXhPRFJWZG9QdFJvdWtudFlkT0dObENZVE95QUg="

    // 이미지 ByteArray 변환 (API로 보낼 이미지 데이터)
    val stream = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream)
    val imageByteArray = stream.toByteArray()

    // 요청 JSON 생성 (request_json)
    val requestJson = JSONObject().apply {
        put("version", "V2")
        put("requestId", UUID.randomUUID().toString())
        put("timestamp", System.currentTimeMillis())
        val image = JSONObject().apply {
            put("format", "jpg")
            put("name", "demo")
        }
        put("images", org.json.JSONArray().put(image))
    }

    // Multipart Body 생성 (이미지, JSON 한 번에 보내기)
    val requestBody = MultipartBody.Builder()
        .setType(MultipartBody.FORM)
        .addFormDataPart("message", requestJson.toString())
        .addFormDataPart("file", "image.jpg",
            imageByteArray.toRequestBody("image/jpeg".toMediaType(), 0, imageByteArray.size))
        .build()

    // HTTP 객체 생성
    val request = Request.Builder()
        .url(apiUrl)
        .header("X-OCR-SECRET", secretKey)
        .post(requestBody)
        .build()

    // API 호출
    OkHttpClient().newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            e.printStackTrace()
            onResult("OCR 실패: ${e.message}")
        }

        override fun onResponse(call: Call, response: Response) {
            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                Log.d("OCR_SUCCESS", responseBody ?: "empty body")
                onResult(responseBody ?: "결과 없음")
            } else {
                val errorBody = response.body?.string()
                Log.e("OCR_ERROR", "Error ${response.code}: $errorBody")
                onResult("OCR 실패: 코드 ${response.code}")
            }
        }
    })
}