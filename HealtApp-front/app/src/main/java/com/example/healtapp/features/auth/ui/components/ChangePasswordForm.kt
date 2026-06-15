package com.example.healtapp.features.auth.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.healtapp.core.ui.components.AppButton
import com.example.healtapp.core.ui.components.AppMessageBanner
import com.example.healtapp.core.ui.components.AppMessageType
import com.example.healtapp.core.ui.components.AppPasswordField

@Composable
fun ChangePasswordForm(
    currentPassword: String,
    newPassword: String,
    confirmPassword: String,
    isChanging: Boolean,
    enabled: Boolean,
    error: String?,
    success: String?,
    onCurrentChange: (String) -> Unit,
    onNewChange: (String) -> Unit,
    onConfirmChange: (String) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
    hint: String = "После сброса по письму укажите код из email как текущий пароль.",
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        AppMessageBanner(text = hint, type = AppMessageType.Info)
        AppPasswordField(
            value = currentPassword,
            onValueChange = onCurrentChange,
            label = "Текущий пароль",
            leadingIcon = Icons.Outlined.Lock,
            enabled = enabled,
        )
        AppPasswordField(
            value = newPassword,
            onValueChange = onNewChange,
            label = "Новый пароль",
            leadingIcon = Icons.Outlined.Lock,
            enabled = enabled,
        )
        AppPasswordField(
            value = confirmPassword,
            onValueChange = onConfirmChange,
            label = "Подтверждение",
            leadingIcon = Icons.Outlined.Lock,
            enabled = enabled,
        )
        error?.let {
            AuthMessageBanner(text = it, type = AuthMessageType.Error)
        }
        success?.let {
            AuthMessageBanner(text = it, type = AuthMessageType.Success)
        }
        AppButton(
            text = if (isChanging) "Меняем пароль…" else "Сменить пароль",
            onClick = onSubmit,
            enabled = enabled && !isChanging,
        )
    }
}

@Composable
fun ChangePasswordFormHeader() {
    Text(
        text = "Новый пароль должен отличаться от текущего. Минимум 6 символов.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.Normal,
    )
}
