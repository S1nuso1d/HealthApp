package com.example.healtapp.features.profile.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.healtapp.core.ui.theme.MintPrimary
import com.example.healtapp.core.ui.theme.SkyPrimary
import com.example.healtapp.core.ui.theme.brandingGradient

@Composable
fun ProfileSaveSuccessOverlay(
    visible: Boolean,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = scaleIn(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium,
            ),
            initialScale = 0.4f,
        ) + fadeIn(),
        exit = scaleOut(targetScale = 0.85f) + fadeOut(),
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .shadow(10.dp, CircleShape, clip = false)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(brandingGradient()),
                )
                .border(
                    width = 2.dp,
                    brush = Brush.linearGradient(
                        listOf(
                            Color.White.copy(alpha = 0.55f),
                            MintPrimary.copy(alpha = 0.35f),
                            SkyPrimary.copy(alpha = 0.35f),
                        ),
                    ),
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = "Сохранено",
                tint = Color.White,
                modifier = Modifier.size(28.dp),
            )
        }
    }
}
