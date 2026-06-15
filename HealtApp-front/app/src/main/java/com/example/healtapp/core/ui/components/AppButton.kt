package com.example.healtapp.core.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.example.healtapp.core.ui.animation.appPressScale
import com.example.healtapp.core.ui.theme.brandingGradient
import com.example.healtapp.core.ui.theme.isAppDarkTheme

@Composable
fun AppButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    isSecondary: Boolean = false,
    modifier: Modifier = Modifier.fillMaxWidth(),
) {
    val interaction = remember { MutableInteractionSource() }

    if (isSecondary) {
        OutlinedButton(
            onClick = onClick,
            enabled = enabled,
            interactionSource = interaction,
            modifier = modifier
                .height(54.dp)
                .appPressScale(interaction, pressedScale = 0.98f),
            shape = RoundedCornerShape(if (isAppDarkTheme()) 4.dp else 18.dp),
            border = ButtonDefaults.outlinedButtonBorder.copy(
                brush = Brush.horizontalGradient(brandingGradient()),
            ),
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    } else {
        Button(
            onClick = onClick,
            enabled = enabled,
            interactionSource = interaction,
            modifier = modifier
                .height(54.dp)
                .appPressScale(interaction, pressedScale = 0.98f),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}
