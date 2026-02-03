package com.example.antiphishingapp.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.example.antiphishingapp.R
import com.example.antiphishingapp.theme.Pretendard
import com.example.antiphishingapp.theme.Primary900
import com.example.antiphishingapp.theme.Primary300

@Composable
fun PhoneAlertCard(
    onStartDetect: () -> Unit,
    onDismiss: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFCFBFF)),
        modifier = Modifier
            .size(width = 344.dp, height = 124.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {

                Icon(
                    painter = painterResource(R.drawable.ic_call),
                    contentDescription = null,
                    tint = Primary900,
                    modifier = Modifier.size(32.dp)
                )

                Spacer(Modifier.width(15.dp))

                Text(
                    text = "전화 수신이 감지되었습니다.",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = Pretendard,
                    color = Primary900
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                // 버튼 사이에 12.dp의 간격을 줍니다.
                horizontalArrangement = Arrangement.spacedBy(17.dp)
            ) {

                Button(
                    onClick = onStartDetect,
                    colors = ButtonDefaults.buttonColors(containerColor = Primary900),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                ) {
                    Text("탐지 시작", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = Primary300),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                ) {
                    Text("알림 지우기", color = Primary900, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PhoneAlertCardPreview() {
    PhoneAlertCard(onStartDetect = {}, onDismiss = {})
}
