package com.example.antiphishingapp.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navDeepLink
import com.example.antiphishingapp.feature.model.AnalysisResponse
import com.example.antiphishingapp.feature.model.VoiceUiResult
import com.example.antiphishingapp.feature.viewmodel.AnalysisViewModel
import com.example.antiphishingapp.feature.viewmodel.LoginViewModel
import com.example.antiphishingapp.feature.viewmodel.VoiceAnalysisViewModel
import com.example.antiphishingapp.ui.main.MainScreen
import com.example.antiphishingapp.ui.screen.*
import com.example.antiphishingapp.feature.viewmodel.AuthViewModel

@Composable
fun AppNavGraph(navController: NavHostController, startRoute: String) {

    val authViewModel: AuthViewModel = viewModel()
    val analysisViewModel: AnalysisViewModel = viewModel()
    val voiceAnalysisViewModel: VoiceAnalysisViewModel = viewModel()

    // 이미지 업로드 결과
    val imageUploadResult = remember { mutableStateOf<AnalysisResponse?>(null) }

    // 음성 업로드 결과 (String → VoiceUiResult 로 변경!)
    val voiceUploadResult = remember { mutableStateOf<VoiceUiResult?>(null) }

    NavHost(
        navController = navController,
        startDestination = startRoute
    ) {
        composable("title") {
            TitleScreen(
                navController = navController,
                authViewModel = authViewModel
            )
        }

        composable("login") {
            LoginScreen(
                navController = navController,
                viewModel = viewModel<LoginViewModel>(),
                authViewModel = authViewModel
            )
        }

        // 메인 화면
        composable("main") {
            MainScreen(
                navController = navController,
                authViewModel = authViewModel,
                onAnalysisComplete = { result ->
                    imageUploadResult.value = result
                    navController.navigate("analysis")
                }
            )
        }

        // 파일 업로드 화면
        composable("fileUpload") {
            FileUploadScreen(
                navController = navController,
                authViewModel = authViewModel,
                analysisViewModel = analysisViewModel,
                voiceAnalysisViewModel = voiceAnalysisViewModel,

                onUploadSuccess = { result ->
                    imageUploadResult.value = result
                    navController.navigate("imageUploadResult")
                },

                onVoiceUploadSuccess = { result ->
                    voiceUploadResult.value = result
                    navController.navigate("voiceUploadResult")
                }
            )
        }

        composable("detectHistory") {
            DetectHistoryScreen(
                navController = navController,
                authViewModel = authViewModel
            )
        }

        composable(
            route = "smsList",
            deepLinks = listOf(navDeepLink { uriPattern = "myapp://sms_list" })
        ) {
            SmsListScreen()
        }

        composable(
            route = "callList",
            deepLinks = listOf(navDeepLink { uriPattern = "myapp://call_list" })
        ) {
            CallListScreen()
        }

        composable("signup") {
            SignUpScreen(
                navController = navController,
                viewModel = viewModel()
            )
        }

        // 이미지 업로드 결과
        composable("imageUploadResult") {
            imageUploadResult.value?.let { result ->
                ImageUploadResultScreen(
                    navController = navController,
                    analysis = result
                )
            }
        }

        // 음성 업로드 결과
        composable("voiceUploadResult") {
            voiceUploadResult.value?.let { result ->
                VoiceUploadResultScreen(
                    navController = navController,
                    riskScore = result.riskScore,
                    suspiciousItems = result.suspiciousItems,
                    transcript = result.transcript
                )
            }
        }

        composable("realtime") {
            RealtimeScreen()
        }
    }
}
