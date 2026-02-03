package com.example.antiphishingapp.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.antiphishingapp.theme.Pretendard
import com.example.antiphishingapp.theme.Primary300
import com.example.antiphishingapp.theme.Primary900

@Composable
fun FailureDialogCard(
    title: String = "문서로 인식되지 않았습니다",
    message: String = "글자를 찾지 못했습니다. \n다른 사진(문서)을 선택해 주세요.",
    confirmText: String = "다시 선택하기",
    onConfirm: () -> Unit,
    confirmEnabled: Boolean = true,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed = interactionSource.collectIsPressedAsState().value

    // ✅ 기본(안 눌림): 연보라 / 눌림: 진보라
    val buttonContainerColor = when {
        !confirmEnabled -> Primary300
        isPressed -> Primary900
        else -> Primary300
    }

    val buttonContentColor = when {
        !confirmEnabled -> Primary900
        isPressed -> Color.White
        else -> Primary900
    }

    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFCFBFF)),
        modifier = Modifier.width(344.dp).height(144.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = title,
                    fontFamily = Pretendard,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    color = Primary900,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = message,
                    fontFamily = Pretendard,
                    fontWeight = FontWeight.Normal,
                    fontSize = 14.sp,
                    color = Primary900,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Button(
                onClick = onConfirm,
                enabled = confirmEnabled,
                interactionSource = interactionSource,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.width(309.dp).height(40.dp).align(Alignment.CenterHorizontally),
                colors = ButtonDefaults.buttonColors(
                    containerColor = buttonContainerColor,
                    contentColor = buttonContentColor,
                    disabledContainerColor = Primary300,
                    disabledContentColor = Primary900
                )
            ) {
                Text(
                    text = confirmText,
                    fontFamily = Pretendard,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun FailureDialogPreview() {
    MaterialTheme {
        Box(
            modifier = Modifier
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            FailureDialogCard(
                onConfirm = {},
                confirmEnabled = true
            )
        }
    }
}