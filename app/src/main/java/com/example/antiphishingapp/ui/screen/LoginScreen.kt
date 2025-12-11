package com.example.antiphishingapp.ui.screen

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.antiphishingapp.R
import com.example.antiphishingapp.feature.viewmodel.LoginViewModel
import com.example.antiphishingapp.theme.Grayscale100
import com.example.antiphishingapp.theme.Grayscale600
import com.example.antiphishingapp.theme.Primary100
import com.example.antiphishingapp.theme.Primary300
import com.example.antiphishingapp.theme.Primary900
import com.example.antiphishingapp.theme.Pretendard
import androidx.compose.runtime.collectAsState
import com.example.antiphishingapp.feature.viewmodel.AuthViewModel


@Composable
fun LoginScreen(
    navController: NavController,
    viewModel: LoginViewModel = viewModel(),
    authViewModel: AuthViewModel
) {
    val email by viewModel.email.observeAsState("")
    val password by viewModel.password.observeAsState("")
    val isLoading by viewModel.isLoading.observeAsState(false)
    val error by viewModel.error.observeAsState(null)
    val loginSuccess by viewModel.loginSuccess.observeAsState(false)
    val isAutoLoginChecked by viewModel.isAutoLoginChecked.observeAsState(false)
    val context = LocalContext.current

    val authIsLoading by authViewModel.isLoading.collectAsState()
    val combinedIsLoading = isLoading || authIsLoading

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isHovered by interactionSource.collectIsHoveredAsState()

    val isActive = isPressed || isHovered
    val finalIsActive = isActive && !combinedIsLoading

    val buttonContainerColor = if (finalIsActive) Primary900 else Primary300
    val buttonContentColor = if (finalIsActive) Primary100 else Primary900

    // 로그인 성공 시 메인 화면으로 이동
    LaunchedEffect(loginSuccess) {
        if (loginSuccess) {
            authViewModel.reloadUser()
            navController.navigate("main") {
                popUpTo("title") { inclusive = true }
            }
        }
    }

    // 오류 발생 시 토스트 메시지 표시
    LaunchedEffect(error) {
        error?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.onErrorShown()
        }
    }


    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Spacer(modifier = Modifier.height(100.dp))

            // 상단 로고 영역
            Image(
                painter = painterResource(id = R.drawable.ic_savephishing_logo),
                contentDescription = "구해줘 피싱 로고",
                contentScale = ContentScale.FillWidth,
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .padding(bottom = 50.dp) // 입력 필드와의 간격
            )

            // 아이디 (이메일) 입력 필드
            LoginInputField(
                value = email,
                onValueChange = viewModel::onEmailChange,
                label = "이메일",
                icon = Icons.Default.Person,
                keyboardType = KeyboardType.Email,
                enabled = !combinedIsLoading
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 비밀번호 입력 필드
            LoginInputField(
                value = password,
                onValueChange = viewModel::onPasswordChange,
                label = "비밀번호",
                icon = Icons.Default.Lock,
                isPassword = true,
                enabled = !combinedIsLoading
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 자동 로그인 체크박스
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = isAutoLoginChecked,
                    onCheckedChange = viewModel::onAutoLoginCheckedChange,
                    colors = CheckboxDefaults.colors(checkedColor = Primary900),
                    enabled = !combinedIsLoading
                )
                Text(
                    "자동 로그인",
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = Pretendard),
                    color = Grayscale600
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 로그인하기 버튼
            Button(
                onClick = viewModel::onLoginClicked,
                enabled = !combinedIsLoading,
                interactionSource = interactionSource,
                colors = ButtonDefaults.buttonColors(
                    containerColor = buttonContainerColor,
                    contentColor = buttonContentColor,
                    disabledContainerColor = Primary300,
                    disabledContentColor = Primary900
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                if (combinedIsLoading) {
                    CircularProgressIndicator(
                        color = buttonContentColor,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Text(
                        "로그인하기",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = Pretendard
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 비밀번호 분실 링크
            Text(
                text = "비밀번호를 분실하셨나요?",
                color = Grayscale600,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.clickable { navController.navigate("reset_password") } // TODO: 비밀번호 재설정 화면 경로 필요
            )
        }
    }
}


@Composable
private fun LoginInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    isPassword: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    enabled: Boolean
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = { Icon(imageVector = icon, contentDescription = null, tint = Primary900) },
        visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = if (isPassword) KeyboardType.Password else keyboardType),
        singleLine = true,
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Grayscale100,
            unfocusedContainerColor = Grayscale100,
            disabledContainerColor = Grayscale100,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
            cursorColor = Primary900
        ),
        modifier = Modifier.fillMaxWidth()
    )
}