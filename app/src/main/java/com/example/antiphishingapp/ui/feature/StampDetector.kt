package com.example.antiphishingapp.feature

import android.content.Context
import android.graphics.Bitmap
import android.widget.Toast
import com.example.antiphishingapp.feature.model.DetectionResult
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgproc.Imgproc

object StampDetector {
    fun findStampRoi(inputBitmap: Bitmap, context: Context): DetectionResult {
        val srcMat = Mat()
        Utils.bitmapToMat(inputBitmap, srcMat) // 비트맵 → Mat 변환

        val bgrMat = Mat()
        Imgproc.cvtColor(srcMat, bgrMat, Imgproc.COLOR_RGBA2BGR)

        val hsvMat = Mat()
        Imgproc.cvtColor(bgrMat, hsvMat, Imgproc.COLOR_BGR2HSV)

        // 빨간색 범위 (직인 색)
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

        // 모폴로지 연산 (노이즈 제거)
        val kernel = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, Size(5.0, 5.0))
        Imgproc.morphologyEx(redMask, redMask, Imgproc.MORPH_CLOSE, kernel)
        Imgproc.morphologyEx(redMask, redMask, Imgproc.MORPH_OPEN, kernel)

        // 컨투어 탐색
        val contours = ArrayList<MatOfPoint>()
        val hierarchy = Mat()
        Imgproc.findContours(redMask, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE)

        val rects = mutableListOf<Rect>()
        if (contours.isNotEmpty()) {
            for (contour in contours) {
                if (Imgproc.contourArea(contour) > 1000) { // 작은 영역은 무시
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
}
