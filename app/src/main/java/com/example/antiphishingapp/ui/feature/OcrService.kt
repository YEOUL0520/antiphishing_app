package com.example.antiphishingapp.feature

import android.graphics.Bitmap
import android.util.Log
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.*

object OcrService {
    private const val apiUrl = "https://fwymjktetd.apigw.ntruss.com/custom/v1/45162/f06f44fc9667be94a98feed9824ad4f1bb0c7a35bf9e32132fc012be76435739/general"
    private const val secretKey = "S0daUXhPRFJWZG9QdFJvdWtudFlkT0dObENZVE95QUg="

    fun performOcr(bitmap: Bitmap, onResult: (String) -> Unit) {
        // Bitmap → ByteArray 변환
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream)
        val imageByteArray = stream.toByteArray()

        // 요청 JSON
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

        // Multipart body
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("message", requestJson.toString())
            .addFormDataPart(
                "file", "image.jpg",
                imageByteArray.toRequestBody("image/jpeg".toMediaType(), 0, imageByteArray.size)
            )
            .build()

        val request = Request.Builder()
            .url(apiUrl)
            .header("X-OCR-SECRET", secretKey)
            .post(requestBody)
            .build()

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
}
