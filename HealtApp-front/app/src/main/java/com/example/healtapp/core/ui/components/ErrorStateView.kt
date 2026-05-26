package com.example.healtapp.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ErrorStateView(
    message: String,
    onRetry: (() -> Unit)? = null,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        AppMessageBanner(
            text = message,
            type = AppMessageType.Error,
            title = "Что-то пошло не так",
            modifier = Modifier.padding(horizontal = 8.dp),
        )

        if (onRetry != null) {
            Spacer(modifier = Modifier.height(14.dp))
            AppButton(text = "Повторить", onClick = onRetry)
        }
    }
}