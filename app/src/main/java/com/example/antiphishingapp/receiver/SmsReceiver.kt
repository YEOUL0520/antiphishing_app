package com.example.antiphishingapp.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.example.antiphishingapp.data.local.AppDatabase
import com.example.antiphishingapp.data.local.SmsEntity
import com.example.antiphishingapp.network.ApiClient
import com.example.antiphishingapp.network.SmsDetectRequest
import com.example.antiphishingapp.network.SmsDetectResponse
import com.example.antiphishingapp.ui.AlertActivity
import com.example.antiphishingapp.utils.SaltKeeper
import com.example.antiphishingapp.utils.Sanitizer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        try {
            if (intent?.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
                val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
                var sender: String? = null
                val sb = StringBuilder()
                for (msg in messages) {
                    sender = msg.originatingAddress
                    sb.append(msg.messageBody)
                }
                val rawText = sb.toString().trim()
                Log.d("SmsReceiver", "📩 Received SMS: $sender / ${rawText.take(80)}...")

                val smsStartTime = System.currentTimeMillis()
                Log.d("SmsReceiver", "⏱️ [PERF] 1. SMS_RECEIVE_START: $smsStartTime")

                // 비동기로 서버 전송
                CoroutineScope(Dispatchers.IO).launch {
                    sendToServer(context!!, sender ?: "unknown", rawText, smsStartTime)
                }
            }
        } catch (e: Exception) {
            Log.e("SmsReceiver", "onReceive error: ${e.message}")
        }
    }

    private fun sendToServer(context: Context, sender: String, rawText: String, startTime: Long) {
        try {
            // 1️⃣ 해시 생성
            val salt = SaltKeeper.getSalt(context)
            val senderHash = Sanitizer.sha256Hash(sender, salt)

            // 2️⃣ URL 추출 및 나머지 텍스트 분리
            val urls = Sanitizer.extractUrls(rawText)
            val textOnly = Sanitizer.removeUrls(rawText)
            val texts = Sanitizer.splitToSentences(textOnly)

            // 3️⃣ 요청 모델 구성
            val payload = SmsDetectRequest(
                sender_hash = senderHash,
                urls = urls,
                texts = texts,
                received_at = System.currentTimeMillis()
            )

            // 4️⃣ 서버 전송

            val apiStartTime = System.currentTimeMillis()
            Log.d("SmsReceiver", "⏱️ [PERF] 2. API_REQUEST_START: $apiStartTime")

            ApiClient.apiService.detectSmsJson(payload).enqueue(object :
                Callback<SmsDetectResponse> {
                override fun onResponse(
                    call: Call<SmsDetectResponse>,
                    response: Response<SmsDetectResponse>
                ) {
                    if (response.isSuccessful) {

                        val endTime = System.currentTimeMillis()
                        val apiLatency = endTime - apiStartTime
                        val totalLatency = endTime - startTime
                        Log.d("SmsReceiver", "⏱️ [PERF] 3. API_RESPONSE_RECEIVED")
                        Log.d("SmsReceiver", "📊 [RESULT] SMS API 응답 시간: ${apiLatency}ms")
                        Log.d("SmsReceiver", "📊 [RESULT] SMS 전체 처리 시간: ${totalLatency}ms")

                        val result = response.body()
                        val score = (result?.phishing_score as? Number)?.toInt() ?: 0
                        val foundKeywords = result?.keywords_found ?: emptyList()
                        Log.d(
                            "SmsReceiver",
                            "✅ Phishing=${result?.phishing_score}, keywords=${result?.keywords_found}, urls=${result?.url_results?.size}"
                        )

                        if (score >= 70) {
                            val popupIntent = Intent(context, AlertActivity::class.java).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                putExtra("type", "sms")
                            }
                            context.startActivity(popupIntent)
                            Log.d("SmsReceiver", "🚨 위험 감지! 알림창 실행됨 (점수: $score)")

                            CoroutineScope(Dispatchers.IO).launch {
                                val db = AppDatabase.Companion.getDatabase(context)
                                db.smsDao().insertSms(
                                    SmsEntity(
                                        sender = sender,
                                        content = rawText,
                                        receivedDate = System.currentTimeMillis(),
                                        riskScore = score,
                                        keywords = foundKeywords
                                    )
                                )
                                Log.d("SmsReceiver", "💾 DB 저장 완료")
                            }
                        } else {
                            Log.d("SmsReceiver", "🛡️ 안전한 문자입니다. 알림을 띄우지 않습니다. (점수: $score)")
                        }

                    } else {
                        Log.e("SmsReceiver", "❌ Server error: ${response.code()}")
                    }
                }

                override fun onFailure(call: Call<SmsDetectResponse>, t: Throwable) {
                    Log.e("SmsReceiver", "🚨 Network failure: ${t.message}")
                }
            })
        } catch (e: Exception) {
            Log.e("SmsReceiver", "sendToServer error: ${e.message}")
        }
    }
}