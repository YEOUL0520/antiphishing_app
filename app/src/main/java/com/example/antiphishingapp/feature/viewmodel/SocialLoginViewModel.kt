package com.example.antiphishingapp.feature.viewmodel

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.antiphishingapp.network.AppConfig
import com.example.antiphishingapp.feature.repository.AuthRepository
import com.example.antiphishingapp.ui.SocialLoginCallbackHandler
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch

// 로그인 결과 상태 정의
sealed class LoginResult {
    object Success : LoginResult()
    object SuccessWithInfo : LoginResult()
    object Failure : LoginResult()
}

/**
 * 서버 주도 소셜 로그인 ViewModel
 * - 로그인 URL 제공
 * - 딥링크 콜백 파싱
 * - access/refresh token 저장
 * - 신규가입 여부(requires_additional_info) 처리
 */
class SocialLoginViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val authRepository = AuthRepository(application)

    private val _loginResult = MutableSharedFlow<LoginResult>()
    val loginResult: SharedFlow<LoginResult> = _loginResult



    // [수정] final_redirect_uri 쿼리 파라미터 포함
    fun getKakaoAuthUrl(): String {
        return "${AppConfig.KAKAO_LOGIN_URL}?final_redirect_uri=${AppConfig.KAKAO_FINAL_REDIRECT_URI}"
    }

    fun getNaverAuthUrl(): String {
        return "${AppConfig.NAVER_LOGIN_URL}?final_redirect_uri=${AppConfig.NAVER_FINAL_REDIRECT_URI}"
    }

    /**
     * 서버에서 보내준 딥링크 파싱
     * 예: antiphishingapp://oauth/kakao/callback?access_token=xxx&refresh_token=yyy&requires_additional_info=true
     */
    suspend fun handleCallbackUri(uri: Uri) {
        val accessToken = uri.getQueryParameter("access_token")
        val refreshToken = uri.getQueryParameter("refresh_token")
        val requiresAdditionalInfo =
            uri.getQueryParameter("requires_additional_info")?.toBoolean() ?: false
        val error = uri.getQueryParameter("error")

        // 에러 처리
        if (error != null) {
            Log.e("SOCIAL_LOGIN", "Server returned error during login: $error")
            _loginResult.emit(LoginResult.Failure)
            return
        }

        // 토큰 누락
        if (accessToken == null || refreshToken == null) {
            Log.e("SOCIAL_LOGIN", "Missing tokens in callback URI.")
            _loginResult.emit(LoginResult.Failure)
            return
        }

        try {
            // 토큰 저장
            authRepository.saveTokens(accessToken, refreshToken, true)

            // 신규 가입자 → 추가 정보 필요
            if (requiresAdditionalInfo) {
                _loginResult.emit(LoginResult.SuccessWithInfo)
            } else {
                _loginResult.emit(LoginResult.Success)
            }

        } catch (e: Exception) {
            Log.e("SOCIAL_LOGIN", "Token save error: ${e.message}")
            _loginResult.emit(LoginResult.Failure)
        }
    }
}
