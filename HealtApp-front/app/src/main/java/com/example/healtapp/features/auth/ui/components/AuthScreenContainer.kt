package com.example.healtapp.features.auth.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.healtapp.R

val AuthTopColor = Color(0xFFEFFFFA)
val AuthBottomColor = Color(0xFFEAF6FF)
val AuthAccent = Color(0xFF66D19E)
val AuthAccentBlue = Color(0xFF72B8FF)
val AuthCardColor = Color(0xF9FFFFFF)
val AuthBorderColor = Color(0x22000000)

@Composable
fun AuthScreenContainer(
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(AuthTopColor, Color.White, AuthBottomColor)
                )
            )
    ) {
        Image(
            painter = painterResource(id = R.drawable.logo_healthapp),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.Center)
                .size(280.dp)
                .alpha(0.10f)
        )

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
            content = content
        )

        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            color = Color.Transparent
        ) {
            Text(
                text = "HealthApp",
                modifier = Modifier.padding(bottom = 18.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0x99000000),
                fontWeight = FontWeight.Medium
            )
        }
    }
}