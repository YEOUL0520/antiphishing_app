package com.example.antiphishingapp.ui.main

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.antiphishingapp.R
import com.example.antiphishingapp.feature.model.AnalysisResponse
import com.example.antiphishingapp.theme.AntiPhishingAppTheme
import com.example.antiphishingapp.theme.AppTypography
import com.example.antiphishingapp.theme.Grayscale300
import com.example.antiphishingapp.theme.Grayscale600
import com.example.antiphishingapp.theme.Grayscale700
import com.example.antiphishingapp.theme.Grayscale800
import com.example.antiphishingapp.theme.Grayscale900
import com.example.antiphishingapp.theme.NPSFont
import com.example.antiphishingapp.theme.Primary100
import com.example.antiphishingapp.theme.Primary300
import com.example.antiphishingapp.theme.Primary900
import com.example.antiphishingapp.feature.viewmodel.AuthViewModel


@Composable
fun MainScreen(
    navController: NavController,
    onAnalysisComplete: (AnalysisResponse) -> Unit,
    authViewModel: AuthViewModel
) {
    val userState by authViewModel.user.collectAsState()
    val userName = userState?.fullName ?: "사용자"

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Primary100
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            TopBar(userName = userName)
            Spacer(modifier = Modifier.height(103.dp))

            Greeting(userName = userName)
            Spacer(modifier = Modifier.height(32.dp))

            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                ActionCard(
                    title = "파일 업로드",
                    description = "문서 이미지 캡쳐, 음성 메시지의\n피싱 위험도 확인이 가능합니다.",
                    iconRes = R.drawable.mainscreen01,
                    onClick = { navController.navigate("fileUpload") }
                )
                Spacer(modifier = Modifier.height(25.dp))

                ActionCard(
                    title = "탐지 기록 확인",
                    description = "의심 전화 및 문자를 탐지하고,\n위험도를 확인할 수 있습니다.",
                    iconRes = R.drawable.mainscreen02,
                    onClick = { navController.navigate("detectHistory") }
                )

                HelpSection(modifier = Modifier.padding(vertical = 64.dp))
            }
        }
    }
}

@Composable
fun TopBar(userName: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.clickable { /* No action */ },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Grayscale300)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = userName,
                style = AppTypography.titleMedium,
                color = Grayscale800
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        IconButton(onClick = { /* No action */ }) {
            Icon(
                imageVector = Icons.Default.Menu,
                contentDescription = "Menu",
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun Greeting(userName:

             String) {
    Column {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = userName,
                style = AppTypography.headlineLarge.copy(
                    fontFamily = NPSFont,
                    fontWeight = FontWeight.Normal
                ),
                color = Primary900,
            )
            Text(
                text = "님,",
                style = AppTypography.headlineLarge.copy(
                    fontFamily = NPSFont,
                    fontWeight = FontWeight.Normal
                ),
                color = Grayscale900,
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "보안 탐지를 시작하세요.",
            style = AppTypography.headlineLarge.copy(
                fontFamily = NPSFont,
                fontWeight = FontWeight.Normal
            ),
            color = Grayscale900,
        )
    }
}

@Composable
fun ActionCard(
    title: String,
    description: String,
    @DrawableRes iconRes: Int,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val cardColor = if (isPressed) Primary900 else Primary300
    val titleColor = if (isPressed) Primary300 else Primary900
    val descriptionColor = if (isPressed) Primary300 else Grayscale700

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = AppTypography.headlineLarge.copy(
                        fontFamily = NPSFont,
                        fontWeight = FontWeight.Bold
                    ),
                    color = titleColor
                )
                Spacer(Modifier.height(20.dp))
                Text(
                    text = description,
                    style = AppTypography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = descriptionColor,
                    lineHeight = 20.sp
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Image(
                painter = painterResource(id = iconRes),
                contentDescription = title,
                modifier = Modifier.size(100.dp)
            )
        }
    }
}

@Composable
fun HelpSection(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "도움이 필요하신가요?",
            modifier = Modifier.clickable { /* No action */ },
            style = AppTypography.bodyMedium,
            color = Grayscale600
        )
    }
}