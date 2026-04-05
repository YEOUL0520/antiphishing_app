package com.example.antiphishingapp.feature.realtime

import android.util.Log
import com.example.antiphishingapp.feature.model.RealtimeMessage
import com.google.gson.Gson
import okhttp3.*

class WebSocketClient(
    private val serverUrl: String,
    private val onMessageReceived: (RealtimeMessage) -> Unit
) {
    private val gson = Gson()
    private var webSocket: WebSocket? = null

    private var isFirstCaptionLogged = false

    private val client = OkHttpClient()

    fun connect() {
        val request = Request.Builder().url(serverUrl).build()
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(ws: WebSocket, response: Response) {
                Log.d("WebSocketClient", "WebSocket 연결됨")
            }

            override fun onMessage(ws: WebSocket, text: String) {
                Log.d("WebSocketClient", "📩 수신: $text")   // ← 이 줄 추가
                try {
                    val receiveTime = System.currentTimeMillis()
                    val msg = gson.fromJson(text, RealtimeMessage::class.java)

                    // 1. 첫 자막 표시 시간 측정 (텍스트가 포함된 첫 메시지 기준)
                    if (!isFirstCaptionLogged && !msg.text.isNullOrBlank()) {
                        val firstCaptionLatency = receiveTime - RealtimeCallService.streamStartTime
                        Log.d("WebSocketClient", "⏱️ [PERF] 2. FIRST_CAPTION_RECEIVED")
                        Log.d("WebSocketClient", "📊 [RESULT] 첫 자막 표시 시간: ${firstCaptionLatency}ms")
                        isFirstCaptionLogged = true
                    }

                    // 2. 위험 경고 시간 측정 (위험 점수가 기준치 이상인 경우)
                    val isPhishingDetected = (msg.risk_probability ?: 0.0) >= 70.0 || msg.is_phishing == true
                    if (isPhishingDetected) {
                        val warningLatency = receiveTime - RealtimeCallService.streamStartTime
                        Log.d("WebSocketClient", "⏱️ [PERF] 3. RISK_WARNING_DETECTED")
                        Log.d("WebSocketClient", "📊 [RESULT] 위험 경고 지연 시간(누적): ${warningLatency}ms")
                    }
                    onMessageReceived(msg)
                } catch (e: Exception) {
                    Log.e("WebSocketClient", "메시지 파싱 오류: ${e.message}")
                }
            }

            override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                Log.e("WebSocketClient", "WebSocket 오류: ${t.message}")
            }

            override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                Log.d("WebSocketClient", "WebSocket 종료: $reason")
            }
        })
    }

    fun send(data: String) {
        webSocket?.send(data)
    }

    fun close() {
        webSocket?.close(1000, "Normal closure")
        client.dispatcher.executorService.shutdown()
    }
}
