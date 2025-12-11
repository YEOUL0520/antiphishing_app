package com.example.antiphishingapp.network

/**
 * 서버 기반 소셜 로그인 및 API 통신에 필요한 환경 변수를 정의합니다.
 */
object AppConfig {

    // --- 백엔드 서버 주소 ---
    const val BASE_URL = "http://13.125.25.96:8000/"

    // --- 소셜 로그인 시작 URL ---
    const val KAKAO_LOGIN_URL = "${BASE_URL}auth/kakao/login"
    const val NAVER_LOGIN_URL = "${BASE_URL}auth/naver/login"

    // --- 최종 리디렉션 딥링크 (백엔드에 전달) ---
    const val KAKAO_FINAL_REDIRECT_URI = "antiphishingapp://oauth/kakao/callback"
    const val NAVER_FINAL_REDIRECT_URI = "antiphishingapp://oauth/naver/callback"
}
