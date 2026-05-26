package com.example.healtapp.features.social.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.healtapp.core.ui.components.AppButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacySheet(
    profileVisibility: String,
    feedVisibility: String,
    showActivity: Boolean,
    showAchievements: Boolean,
    onProfileVisibility: (String) -> Unit,
    onFeedVisibility: (String) -> Unit,
    onShowActivity: (Boolean) -> Unit,
    onShowAchievements: (Boolean) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Кто видит профиль", style = MaterialTheme.typography.titleMedium)
            VisibilityChips(profileVisibility, onProfileVisibility)
            Text("Кто видит ленту", style = MaterialTheme.typography.titleMedium)
            VisibilityChips(feedVisibility, onFeedVisibility)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Тренировки друзьям")
                Switch(checked = showActivity, onCheckedChange = onShowActivity)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Достижения друзьям")
                Switch(checked = showAchievements, onCheckedChange = onShowAchievements)
            }
            AppButton(text = "Сохранить", onClick = onSave)
        }
    }
}

@Composable
private fun VisibilityChips(selected: String, onSelect: (String) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("public" to "Все", "friends" to "Друзья", "private" to "Только я").forEach { (v, label) ->
            FilterChip(
                selected = selected == v,
                onClick = { onSelect(v) },
                label = { Text(label) },
            )
        }
    }
}
