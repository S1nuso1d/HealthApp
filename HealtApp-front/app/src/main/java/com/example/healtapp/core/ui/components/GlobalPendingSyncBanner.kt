package com.example.healtapp.core.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun GlobalPendingSyncBanner(
    count: Int,
    isFlushing: Boolean,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (count <= 0) return
    val label = when {
        isFlushing -> "Отправляем записи на сервер…"
        count == 1 -> "1 запись ждёт синхронизации · нажмите, чтобы отправить"
        count in 2..4 -> "$count записи ждут синхронизации · нажмите, чтобы отправить"
        else -> "$count записей ждут синхронизации · нажмите, чтобы отправить"
    }
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = !isFlushing, onClick = onTap),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.75f),
    ) {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.CloudUpload,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
    }
}
