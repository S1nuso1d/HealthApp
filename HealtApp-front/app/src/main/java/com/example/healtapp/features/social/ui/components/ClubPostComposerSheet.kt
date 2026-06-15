package com.example.healtapp.features.social.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Poll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.healtapp.core.ui.components.AppButton
import com.example.healtapp.core.ui.components.GradientFormPanel
import com.example.healtapp.core.ui.components.GradientOutlinedField
import com.example.healtapp.core.ui.components.RoundedSectionTabs
import com.example.healtapp.core.ui.components.RoundedTabItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClubPostComposerSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    onPublish: (postType: String, body: String, pollOptions: List<String>?) -> Unit,
) {
    if (!visible) return

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var composerType by remember { mutableIntStateOf(0) }
    var body by remember { mutableStateOf("") }
    var pollOptions by remember { mutableStateOf("") }

    val typeKey = when (composerType) {
        1 -> "achievement"
        2 -> "poll"
        else -> "discussion"
    }
    val canPublish = body.isNotBlank() || composerType == 2

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "Новая публикация",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "Обсуждение, достижение или опрос для клуба",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Закрыть")
                }
            }

            GradientFormPanel {
                RoundedSectionTabs(
                    tabs = listOf(
                        RoundedTabItem(0, "Обсуждение", Icons.Filled.Chat),
                        RoundedTabItem(1, "Достижение", Icons.Filled.EmojiEvents),
                        RoundedTabItem(2, "Опрос", Icons.Filled.Poll),
                    ),
                    selected = composerType,
                    onSelect = { composerType = it },
                )
                GradientOutlinedField(
                    value = body,
                    onValueChange = { body = it },
                    label = if (composerType == 1) {
                        "Поделитесь методом или результатом"
                    } else {
                        "Текст"
                    },
                    singleLine = false,
                )
                if (composerType == 2) {
                    GradientOutlinedField(
                        value = pollOptions,
                        onValueChange = { pollOptions = it },
                        label = "Варианты через запятую",
                    )
                }
                AppButton(
                    text = "Опубликовать",
                    onClick = {
                        val opts = if (composerType == 2) {
                            pollOptions.split(",").map { it.trim() }.filter { it.isNotBlank() }
                        } else {
                            null
                        }
                        onPublish(typeKey, body, opts)
                        body = ""
                        pollOptions = ""
                        composerType = 0
                        onDismiss()
                    },
                    enabled = canPublish,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
