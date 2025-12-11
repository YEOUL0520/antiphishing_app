package com.example.antiphishingapp.ui.screen

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.layout.ContentScale
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.antiphishingapp.R
import com.example.antiphishingapp.feature.viewmodel.LoginResult
import com.example.antiphishingapp.feature.viewmodel.SocialLoginViewModel
import com.example.antiphishingapp.ui.SocialLoginCallbackHandler
import com.example.antiphishingapp.feature.viewmodel.AuthViewModel
import kotlinx.coroutines.flow.collectLatest
import com.example.antiphishingapp.theme.Primary900
import com.example.antiphishingapp.theme.Primary300
import com.example.antiphishingapp.theme.Primary100
import com.example.antiphishingapp.theme.Pretendard

@Composable
fun TitleScreen(
    navController: NavController,
    authViewModel: AuthViewModel,
    socialViewModel: SocialLoginViewModel = viewModel()
) {

    val context = LocalContext.current // Context 획득

    // 소셜 로그인 시작 함수 (Custom Tabs 사용)
    val startSocialLogin: (String) -> Unit = { url ->
        try {
            val customTabsIntent = CustomTabsIntent.Builder().build()
            customTabsIntent.launchUrl(context, Uri.parse(url)) // Custom Tabs로 URL 실행
        } catch (e: Exception) {
            // Custom Tabs를 사용할 수 없는 경우 일반 브라우저로 폴백
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(browserIntent)
        }
    }

    val callbackUri by SocialLoginCallbackHandler.uriState

    // 딥링크 콜백 URI가 변경될 때마다 소셜 로그인 콜백 처리
    LaunchedEffect(callbackUri) {
        val uri = callbackUri
        if (uri != null) {
            socialViewModel.handleCallbackUri(uri)
            // URI 처리 후 상태를 다시 null로 만들어 중복 처리를 방지
            SocialLoginCallbackHandler.uriState.value = null
        }
    }

    // 로그인 결과에 따른 화면 이동 처리
    LaunchedEffect(Unit) {
        socialViewModel.loginResult.collectLatest { result ->
            when (result) {
                is LoginResult.Success, is LoginResult.SuccessWithInfo -> {
                    // AuthViewModel에 로그인 상태가 변경되었음을 알리고 사용자 정보를 다시 로드
                    authViewModel.reloadUser()
                    // 로그인 성공 시 메인 화면으로 이동 (MainScreen 경로로 가정)
                    navController.navigate("main") {
                        popUpTo("title") { inclusive = true } // 이전 화면 모두 제거
                    }
                }
                is LoginResult.Failure -> {
                    // 로그인 실패 시 토스트 메시지 출력
                    Toast.makeText(context, "소셜 로그인에 실패했습니다. 다시 시도해 주세요.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    Scaffold(
        content = { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Spacer(modifier = Modifier.height(100.dp))

                Image(
                    painter = painterResource(id = R.drawable.ic_savephishing_logo),
                    contentDescription = "구해줘 피싱 로고",
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .padding(bottom = 50.dp)
                )

                Spacer(modifier = Modifier.weight(1f))

                // 하단 버튼 그룹
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp)
                ) {
                    // 일반 로그인 버튼
                    InteractiveAuthButton(
                        text = "로그인",
                        onClick = { navController.navigate("login") }
                    )

                    Spacer(modifier = Modifier.height(16.dp)) // 로그인, 회원가입 버튼 간격

                    // 회원가입 버튼
                    InteractiveAuthButton(
                        text = "회원가입",
                        onClick = { navController.navigate("signup") }
                    )

                    Spacer(modifier = Modifier.height(16.dp)) // 일반 버튼, 소셜 버튼 간격

                    // 카카오 로그인 버튼
                    SocialLoginButton(
                        text = "카카오 로그인",
                        iconRes = R.drawable.ic_kakao_logo,
                        backgroundColor = Color(0xFFFFEB00),
                        contentColor = Color.Black,
                        onClick = {
                            val url = socialViewModel.getKakaoAuthUrl()
                            startSocialLogin(url)
                        }
                    )

                    Spacer(modifier = Modifier.height(16.dp)) // 소셜 버튼 간격

                    // 네이버 로그인 버튼
                    SocialLoginButton(
                        text = "네이버 로그인",
                        iconRes = R.drawable.ic_naver_logo,
                        backgroundColor = Color(0xFF00BF18),
                        contentColor = Color.White,
                        onClick = {
                            val url = socialViewModel.getNaverAuthUrl()
                            startSocialLogin(url)
                        }
                    )
                }
            }
        }
    )
}

// --- 보조 컴포넌트 ---

/**
 * 상태(마우스 오버/클릭)에 따라 색상이 변하는 인증 버튼
 * 기본: 배경 Primary300 / 글자 Primary900
 * 활성: 배경 Primary900 / 글자 Primary100
 */
@Composable
fun InteractiveAuthButton(
    text: String,
    onClick: () -> Unit
) {
    // 인터랙션 상태 감지
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isHovered by interactionSource.collectIsHoveredAsState()

    // 상태에 따른 색상 결정
    // 마우스가 올라가거나(Hover), 눌렸을 때(Press) 색상 변경
    val containerColor = if (isPressed || isHovered) Primary900 else Primary300
    val contentColor = if (isPressed || isHovered) Primary100 else Primary900

    Button(
        onClick = onClick,
        interactionSource = interactionSource,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.SemiBold,
                fontFamily = Pretendard
            )
        )
    }
}

@Composable
fun SocialLoginButton(
    text: String,
    iconRes: Int,
    backgroundColor: Color,
    contentColor: Color,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = backgroundColor,
            contentColor = contentColor
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().height(56.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(id = iconRes),
                contentDescription = text,
                modifier = Modifier.size(24.dp).padding(end = 8.dp) // 아이콘 크기 및 간격 유지
            )
            Text(text = text, style = MaterialTheme.typography.titleMedium.copy(fontFamily = Pretendard))
        }
    }
}