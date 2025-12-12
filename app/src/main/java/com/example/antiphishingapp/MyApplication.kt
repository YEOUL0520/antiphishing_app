package com.example.antiphishingapp

import android.app.Application
import com.example.antiphishingapp.feature.repository.AuthRepository

class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this

        handleFirstRun()
    }

    private fun handleFirstRun() {
        // 앱 실행 여부를 저장하는 일반 SharedPreferences
        val appPrefs = getSharedPreferences("app_init_prefs", MODE_PRIVATE)
        val isFirstRun = appPrefs.getBoolean("is_first_run", true)

        if (isFirstRun) {
            // 앱 최초 실행 / 재설치 시
            // EncryptedSharedPreferences에 저장된 인증 정보 강제 삭제
            val authRepository = AuthRepository(this)
            authRepository.clearTokens()

            // 최초 실행 처리 완료 표시
            appPrefs.edit()
                .putBoolean("is_first_run", false)
                .apply()
        }
    }

    companion object {
        lateinit var instance: MyApplication
            private set
    }
}
