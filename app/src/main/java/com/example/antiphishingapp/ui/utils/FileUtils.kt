package com.example.antiphishingapp.utils

import android.graphics.Bitmap
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import java.io.ByteArrayOutputStream

fun bitmapToMultipart(bitmap: Bitmap, paramName: String = "file"): MultipartBody.Part {
    val stream = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
    val byteArray = stream.toByteArray()

    val requestFile = RequestBody.create("image/jpeg".toMediaTypeOrNull(), byteArray)
    return MultipartBody.Part.createFormData(paramName, "upload.jpg", requestFile)
}
