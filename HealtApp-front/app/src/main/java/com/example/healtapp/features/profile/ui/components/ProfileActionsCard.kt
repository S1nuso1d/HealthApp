package com.example.healtapp.features.profile.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.example.healtapp.core.ui.components.AppButton
import com.example.healtapp.core.ui.components.AppCard

@Composable
fun ProfileActionsCard(
    onEditProfile: () -> Unit = {},
    onManageNotifications: () -> Unit = {},
    onManageIntegrations: () -> Unit = {}
) {
    AppCard {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AppButton(
                text = "Редактировать профиль",
                onClick = onEditProfile
            )
            AppButton(
                text = "Настроить уведомления",
                onClick = onManageNotifications
            )
            AppButton(
                text = "Управление интеграциями",
                onClick = onManageIntegrations
            )
        }
    }
}