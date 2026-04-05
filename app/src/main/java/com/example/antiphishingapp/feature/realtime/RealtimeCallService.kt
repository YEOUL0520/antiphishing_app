package com.example.antiphishingapp.feature.realtime

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.*
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.antiphishingapp.R
import com.example.antiphishingapp.feature.repository.RealtimeRepository
import kotlinx.coroutines.*
import okio.ByteString
import okio.ByteString.Companion.toByteString
import android.media.audiofx.NoiseSuppressor
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.AutomaticGainControl
import androidx.annotation.RequiresPermission

class RealtimeCallService : Service() {

    private var audioRecord: AudioRecord? = null
    companion object {
        var streamStartTime: Long = 0L
        var isFirstChunkSent: Boolean = false
    }
    private var recordJob: Job? = null
    private val repository = RealtimeRepository()
    private val sampleRate = 16000

    override fun onCreate() {
        super.onCreate()
        Log.d("RealtimeCallService", "🎙 Service Created")
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(1, createForegroundNotification())
        startRecordingAndStreaming()
        return START_STICKY
    }

    /**
     * 🎙 AudioRecord 생성 및 마이크 입력 확보
     */
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    private fun createOptimizedAudioRecord(): AudioRecord? {
        val bufferSize = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        if (bufferSize <= 0) {
            Log.e("RealtimeCallService", "❌ 버퍼 사이즈 오류: $bufferSize")
            return null
        }

        val audioSource = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaRecorder.AudioSource.VOICE_COMMUNICATION
        } else {
            MediaRecorder.AudioSource.VOICE_COMMUNICATION
        }

        val record = try {
            AudioRecord(
                audioSource,
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize
            )
        } catch (e: Exception) {
            Log.e("RealtimeCallService", "AudioRecord 생성 실패: ${e.message}")
            return null
        }

        if (record.state != AudioRecord.STATE_INITIALIZED) {
            Log.e("RealtimeCallService", "AudioRecord 초기화 실패 (state=${record.state})")
            record.release()
            return null
        }

        // 🎧 오디오 보조 기능 안전하게 적용
        applyAudioEffectsSafely(record)
        return record
    }

    /**
     * 🎧 NoiseSuppressor / EchoCanceler / AGC 안전 적용
     */
    private fun applyAudioEffectsSafely(record: AudioRecord) {
        val sessionId = record.audioSessionId

        fun safeApply(name: String, block: () -> Unit) {
            try {
                block()
            } catch (e: Exception) {
                Log.w("RealtimeCallService", "$name 실패: ${e.message}")
            }
        }

        safeApply("NoiseSuppressor") {
            if (NoiseSuppressor.isAvailable()) {
                NoiseSuppressor.create(sessionId)?.apply {
                    enabled = true
                    Log.d("RealtimeCallService", "✅ NoiseSuppressor 활성화")
                }
            }
        }

        safeApply("AcousticEchoCanceler") {
            if (AcousticEchoCanceler.isAvailable()) {
                AcousticEchoCanceler.create(sessionId)?.apply {
                    enabled = true
                    Log.d("RealtimeCallService", "✅ AcousticEchoCanceler 활성화")
                }
            }
        }

        safeApply("AutomaticGainControl") {
            if (AutomaticGainControl.isAvailable()) {
                AutomaticGainControl.create(sessionId)?.apply {
                    enabled = true
                    Log.d("RealtimeCallService", "✅ AGC 활성화")
                }
            }
        }
    }

    /**
     * 🎤 오디오 읽어서 WebSocket으로 전송
     */
    private fun startRecordingAndStreaming() {
        if (checkSelfPermission(android.Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e("RealtimeCallService", "권한 없음 → stopSelf()")
            stopSelf()
            return
        }

        repository.connect()
        audioRecord = createOptimizedAudioRecord()

        if (audioRecord == null) {
            Log.e("RealtimeCallService", "AudioRecord 생성 실패, 서비스 종료")
            stopSelf()
            return
        }

        try {
            audioRecord?.startRecording()
            Log.d("RealtimeCallService", "🎧 AudioRecord 시작됨")
        } catch (e: Exception) {
            Log.e("RealtimeCallService", "startRecording 실패: ${e.message}")
            stopSelf()
            return
        }

        recordJob = CoroutineScope(Dispatchers.IO).launch {
            val buffer = ByteArray(2048)
            while (isActive) {
                val bytesRead = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                Log.d(
                    "RealtimeCallService",
                    "bytesRead=$bytesRead, connected=${repository.connected}"
                )
                val maxVal = buffer.maxOrNull()
                Log.d("AudioDebug", "maxVal=$maxVal")
                if (bytesRead > 0) {
                    val chunk: ByteString = buffer.toByteString(0, bytesRead)
                    if (!isFirstChunkSent) {
                        streamStartTime = System.currentTimeMillis()
                        isFirstChunkSent = true
                        Log.d("RealtimeCallService", "⏱️ [PERF] 1. VOICE_STREAM_START: $streamStartTime")
                    }
                    repository.sendPcm(chunk)
                } else {
                    delay(10)
                }
            }
        }
    }

    /** 🔔 포그라운드 알림 */
    private fun createForegroundNotification(): Notification {
        return NotificationCompat.Builder(this, "realtime_channel")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("실시간 보이스피싱 탐지 중")
            .setContentText("통화 음성을 분석하고 있습니다…")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOngoing(true)
            .build()
    }

    /** 🔔 알림 채널 생성 */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "realtime_channel",
                "실시간 보이스피싱 탐지",
                NotificationManager.IMPORTANCE_HIGH
            )
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("RealtimeCallService", "🛑 서비스 종료 및 리소스 정리")
        try {
            // 녹음 중단 및 리소스 해제
            recordJob?.cancel()
            try {
                audioRecord?.stop()
            } catch (e: Exception) {
                Log.w("RealtimeCallService", "audioRecord stop 실패: ${e.message}")
            }
            try {
                audioRecord?.release()
            } catch (e: Exception) {
                Log.w("RealtimeCallService", "audioRecord release 실패: ${e.message}")
            }

            // 1) STT에 '끝' 신호 보내기 -> 2) 잠깐 대기 -> 3) disconnect
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    Log.d("RealtimeCallService", "📤 Sending __END__ to server")
                    repository.sendText("__END__")
                } catch (e: Exception) {
                    Log.w("RealtimeCallService", "__END__ 전송 예외: ${e.message}")
                }

                // 서버가 STT를 flush할 수 있도록 짧게 대기 (200~500ms 권장)
                delay(300)

                try {
                    Log.d("RealtimeCallService", "📴 Calling repository.disconnect()")
                    repository.disconnect()
                } catch (e: Exception) {
                    Log.w("RealtimeCallService", "disconnect 예외: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.e("RealtimeCallService", "리소스 해제 오류: ${e.message}")
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
