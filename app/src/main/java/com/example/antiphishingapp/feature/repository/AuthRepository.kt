package com.example.antiphishingapp.feature.repository

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.antiphishingapp.feature.model.TokenResponse
import com.example.antiphishingapp.feature.model.UserResponse
import com.example.antiphishingapp.network.ApiClient

/**
 * AuthRepository
 * ------------------------
 * 사용자 인증 토큰 (Access Token, Refresh Token)을 안전하게 저장하고 관리하는 Repository.
 * Context를 생성자로 받아 의존성을 주입받습니다.
 */
class AuthRepository(private val context: Context) {

    private val PREFS_NAME = "secure_auth_prefs"
    private val KEY_ACCESS_TOKEN = "access_token"
    private val KEY_REFRESH_TOKEN = "refresh_token"
    private val KEY_IS_AUTO_LOGIN = "is_auto_login"

    private val securePrefs by lazy {
        // MasterKey 생성 (Android KeyStore 사용)
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        // EncryptedSharedPreferences 생성 및 초기화
        EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    // 일반 로그인 (Email/Password) 후 서버로부터 반환받은 토큰 저장
    fun saveTokens(tokenResponse: TokenResponse, isAutoLogin: Boolean) {
        securePrefs.edit().apply {
            putString(KEY_ACCESS_TOKEN, tokenResponse.accessToken)
            putString(KEY_REFRESH_TOKEN, tokenResponse.refreshToken)
            putBoolean(KEY_IS_AUTO_LOGIN, isAutoLogin)
            apply()
        }
    }

    // 소셜 로그인 후 딥링크로 받은 토큰 저장
    fun saveTokens(accessToken: String, refreshToken: String, isAutoLogin: Boolean) {
        securePrefs.edit().apply {
            putString(KEY_ACCESS_TOKEN, accessToken)
            putString(KEY_REFRESH_TOKEN, refreshToken)
            putBoolean(KEY_IS_AUTO_LOGIN, isAutoLogin)
            apply()
        }
    }


    // 저장된 Access Token을 반환
    fun getAccessToken(): String? {
        return securePrefs.getString(KEY_ACCESS_TOKEN, null)
    }

    // 저장된 Refresh Token을 반환
    fun getRefreshToken(): String? {
        return securePrefs.getString(KEY_REFRESH_TOKEN, null)
    }

    // 토큰이 유효한지, 자동 로그인이 설정되어 있는지 확인
    fun isAuthenticated(): Boolean {
        val hasToken = getAccessToken() != null
        val isAutoLogin = securePrefs.getBoolean(KEY_IS_AUTO_LOGIN, false)

        return hasToken && isAutoLogin
    }

    // 저장된 모든 토큰을 삭제 (로그아웃)
    fun clearTokens() {
        securePrefs.edit().clear().apply()
    }

    // 로그인한 사용자의 정보를 가져옴
    suspend fun getMe(): UserResponse? {
        val token = getAccessToken() ?: return null

        try {
            val response = ApiClient.apiService.getMe("Bearer $token")
            return if (response.isSuccessful) {
                response.body()
            } else {
                null
            }
        } catch (e: Exception) {
            return null
        }
    }
}