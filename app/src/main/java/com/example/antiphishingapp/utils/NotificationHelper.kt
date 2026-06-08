package com.example.antiphishingapp.utils

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.antiphishingapp.R
import com.example.antiphishingapp.feature.realtime.RealtimeCallService
import com.example.antiphishingapp.ui.MainActivity

object NotificationHelper {

    private const val CHANNEL_ID_SMS = "sms_alerts"
    private const val CHANNEL_ID_CALL = "call_detection_channel"

    /* ---------------------------------------------------
     * 1. 문자 관련 알림
     * --------------------------------------------------- */
    fun createChannel(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (nm.getNotificationChannel(CHANNEL_ID_SMS) == null) {
            val channel = NotificationChannel(
                CHANNEL_ID_SMS,
                "SMS 알림",
                NotificationManager.IMPORTANCE_HIGH
            )
            channel.description = "의심 문자 탐지 알림 채널"
            nm.createNotificationChannel(channel)
        }
    }

    fun showSmsAlert(
        context: Context,
        title: String,
        body: String,
        id: Int = (1000..9999).random()
    ) {
        // Android 13+ 알림 권한 체크
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED

            if (!granted) return
        }

        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return
        createChannel(context)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID_SMS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        NotificationManagerCompat.from(context).notify(id, builder.build())
    }

    /* ---------------------------------------------------
     * 2. 보이스피싱 탐지 / 전화 관련 채널
     * --------------------------------------------------- */
    fun createCallChannel(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (nm.getNotificationChannel(CHANNEL_ID_CALL) == null) {
            val channel = NotificationChannel(
                CHANNEL_ID_CALL,
                "보이스피싱 탐지",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "전화 수신 시 보이스피싱 탐지 알림"
                enableLights(true)
                lightColor = Color.RED
                enableVibration(true)
            }

            nm.createNotificationChannel(channel)
        }
    }

    /* ---------------------------------------------------
     * 3. 탐지 시작 알림 (사용자 수동 시작 버튼)
     * --------------------------------------------------- */
    fun showPhishingAlert(context: Context) {
        createCallChannel(context)

        // Android 13+ 알림 권한 체크
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED

            if (!granted) return
        }

        // RealtimeCallService 실행 Intent
        val intent = Intent(context, RealtimeCallService::class.java)

        val pendingIntent = PendingIntent.getService(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID_CALL)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("보이스피싱 탐지 준비됨")
            .setContentText("탐지를 시작하려면 눌러주세요.")
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        NotificationManagerCompat.from(context).notify(
            2001,
            builder.build()
        )
    }


    /* ---------------------------------------------------
 * 4. 통화 종료 알림
 * --------------------------------------------------- */
    fun showCallEndedNotification(context: Context) {
        createCallChannel(context)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return

        // ── 알림 터치 시 앱 열고 음성 업로드 화면으로 이동 ──────────
        val intent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra("navigate_to", "voiceUpload")  // 이동할 화면 전달
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        // ────────────────────────────────────────────────────────────

        val builder = NotificationCompat.Builder(context, CHANNEL_ID_CALL)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("통화가 종료되었습니다")
            .setContentText("음성 파일을 업로드하여 보이스피싱 위험도를 분석해보세요.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)  // ← 추가

        NotificationManagerCompat.from(context).notify(2002, builder.build())
    }
}