package com.example.antiphishingapp.ui.main // 패키지 위치가 ui/main으로 변경됨

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
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.antiphishingapp.ui.theme.AntiPhishingAppTheme
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint
import org.opencv.core.Scalar
import org.opencv.imgproc.Imgproc

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // OpenCV 라이브러리 로드
        if (OpenCVLoader.initDebug()) {
            Log.d("MainActivity", "OpenCV initialized successfully")
        } else {
            Log.e("MainActivity", "OpenCV initialization failed")
        }

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
    var imageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    val context = LocalContext.current

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val inputStream = context.contentResolver.openInputStream(it)
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            imageBitmap = findStampRoi(originalBitmap, context)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (imageBitmap != null) {
            Image(
                bitmap = imageBitmap!!.asImageBitmap(),
                contentDescription = "분석된 이미지",
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
    }
}

// --- Preview ---

@Preview(showBackground = true, name = "Default Preview")
@Composable
fun DefaultPreview() {
    AntiPhishingAppTheme {
        PhishingDetectScreen()
    }
}

// --- Logic ---

private fun findStampRoi(inputBitmap: Bitmap, context: Context): Bitmap {
    val srcMat = Mat()
    val resultBitmap = inputBitmap.copy(Bitmap.Config.ARGB_8888, true)
    Utils.bitmapToMat(resultBitmap, srcMat)

    // ▼▼▼▼▼▼▼▼▼▼▼ 여기가 수동 SDK 방식의 핵심입니다! ▼▼▼▼▼▼▼▼▼▼▼
    val bgrMat = Mat()
    val hsvMat = Mat()

    // 1단계: 안드로이드 RGBA 형식을 OpenCV의 표준인 BGR 형식으로 변환
    Imgproc.cvtColor(srcMat, bgrMat, Imgproc.COLOR_RGBA2BGR)

    // 2단계: BGR 형식을 HSV 형식으로 변환
    Imgproc.cvtColor(bgrMat, hsvMat, Imgproc.COLOR_BGR2HSV)
    // ▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲

    // 너그러운 빨간색 범위 설정
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

    // 노이즈 제거
    val kernel = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, org.opencv.core.Size(5.0, 5.0))
    Imgproc.morphologyEx(redMask, redMask, Imgproc.MORPH_CLOSE, kernel)
    Imgproc.morphologyEx(redMask, redMask, Imgproc.MORPH_OPEN, kernel)

    val contours = ArrayList<MatOfPoint>()
    val hierarchy = Mat()
    Imgproc.findContours(redMask, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE)

    var found = false
    if (contours.isNotEmpty()) {
        for (contour in contours) {
            if (Imgproc.contourArea(contour) > 1000) {
                val rect = Imgproc.boundingRect(contour)
                Imgproc.rectangle(srcMat, rect.tl(), rect.br(), Scalar(0.0, 0.0, 255.0), 8)
                found = true
            }
        }
    }

    if (!found) {
        Toast.makeText(context, "직인 영역을 찾지 못했습니다.", Toast.LENGTH_SHORT).show()
    }

    Utils.matToBitmap(srcMat, resultBitmap)
    return resultBitmap
}