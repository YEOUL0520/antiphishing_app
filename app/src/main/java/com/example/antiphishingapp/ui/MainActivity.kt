package com.example.antiphishingapp.ui

import android.Manifest
import android.content.pm.PackageManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.compose.rememberNavController
import com.example.antiphishingapp.feature.repository.AuthRepository
import com.example.antiphishingapp.theme.AntiPhishingAppTheme
import com.example.antiphishingapp.ui.navigation.AppNavGraph
import com.example.antiphishingapp.utils.NotificationHelper
import org.opencv.android.OpenCVLoader

class MainActivity : ComponentActivity() {

    private lateinit var authRepository: AuthRepository

    /** 단일 권한 요청 런처 (이미지 권한 요청에 사용) */
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                Log.d("PERMISSION", "권한 허용됨")
            } else {
                Toast.makeText(this, "권한이 필요합니다", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        handleSocialLoginIntent(intent)

        // AuthRepository 초기화
        authRepository = AuthRepository(applicationContext)

        // OpenCV 초기화
        if (OpenCVLoader.initDebug()) {
            Log.d("OpenCV", "OpenCV 초기화 성공")
        } else {
            Log.e("OpenCV", "OpenCV 초기화 실패")
        }

        // 🔥 앱 오버레이 권한 요청
        requestOverlayPermission()

        // 🔥 전화 + 마이크 권한 요청
        checkCallPermissions()

        // 🔥 문자 권한 요청
        checkSmsPermission()

        // 🔥 이미지 권한 요청
        checkImagePermission()

        // 🔥 알림 권한 요청 (안드로이드 13+)
        checkNotificationPermission()

        // 🔔 알림 채널 생성
        NotificationHelper.createChannel(this)

        // 🧭 Compose Navigation 설정
        setContent {
            val startDestination = remember {
                if (authRepository.isAuthenticated()) "main" else "title"
            }

            AntiPhishingAppTheme {
                val navController = rememberNavController()

                Surface(
                    modifier = Modifier,
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavGraph(navController, startDestination)
                }
            }
        }
    }

    /**
     * 📌 전화 감지 + 녹음 권한 요청
     * READ_PHONE_STATE / RECORD_AUDIO
     */
    private fun checkCallPermissions() {
        val permissions = arrayOf(
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.RECORD_AUDIO
        )

        val notGranted = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (notGranted.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, notGranted.toTypedArray(), 103)
        } else {
            Log.d("PERMISSION", "전화/녹음 권한 이미 허용됨")
        }
    }

    /**
     * 📩 SMS 관련 권한 요청
     */
    private fun checkSmsPermission() {
        val smsPermissions = arrayOf(
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.READ_SMS
        )

        val notGranted = smsPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (notGranted.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, notGranted.toTypedArray(), 101)
        } else {
            Log.d("PERMISSION", "문자 관련 권한 이미 허용됨")
        }
    }

    /**
     * 🖼 이미지 접근 권한 요청
     */
    private fun checkImagePermission() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        when {
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED -> {
                Log.d("PERMISSION", "이미지 권한 이미 허용됨")
            }

            else -> {
                requestPermissionLauncher.launch(permission)
            }
        }
    }

    /**
     * 🔔 알림 권한 요청 (Android 13+)
     */
    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = Manifest.permission.POST_NOTIFICATIONS

            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(permission), 102)
            } else {
                Log.d("PERMISSION", "알림 권한 이미 허용됨")
            }
        }
    }

    /**
     * 🪟 오버레이 권한 요청
     */
    private fun requestOverlayPermission() {
        if (!android.provider.Settings.canDrawOverlays(this)) {
            val intent = Intent(
                android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
        }
    }

    /**
     * 📌 앱 실행 중 URI 딥링크 처리 (소셜 로그인용)
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleSocialLoginIntent(intent)
    }

    private fun handleSocialLoginIntent(intent: Intent?) {
        val uri: Uri? = intent?.data
        if (intent?.action == Intent.ACTION_VIEW && uri != null) {
            if (uri.scheme == "antiphishingapp") {
                Log.d("SOCIAL_LOGIN", "Received callback URI: $uri")
                SocialLoginCallbackHandler.handleUri(uri)
            }
        }
    }
}

// ───────────────────────────────────────────────────────────────
// 🔗 소셜 로그인 딥링크 콜백 처리
// ───────────────────────────────────────────────────────────────

object SocialLoginCallbackHandler {
    val uriState = mutableStateOf<Uri?>(null)

    fun handleUri(uri: Uri) {
        uriState.value = uri
    }
}
