package com.example.healtapp.features.activity.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.healtapp.core.ui.components.AppButton
import com.example.healtapp.core.ui.components.AppCard
import com.example.healtapp.core.ui.components.AppTextField
import com.example.healtapp.core.ui.theme.brandingGradient
import com.example.healtapp.core.ui.theme.contentPrimaryColor
import com.example.healtapp.features.activity.live.WorkoutNotesCodec
import com.example.healtapp.features.activity.live.WorkoutSessionMeta
import com.example.healtapp.features.activity.live.WorkoutWeatherFetcher
import com.example.healtapp.features.activity.live.WorkoutWeatherInfo
import com.example.healtapp.features.social.ui.components.createSocialCameraUri
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class WorkoutSummaryData(
    val activityId: Int? = null,
    val activityTitleRu: String,
    val startEpochMs: Long,
    val endEpochMs: Long,
    val durationMinutes: Int,
    val distanceKm: Float,
    val calories: Float,
    val avgPaceMinPerKm: Double? = null,
    val cadenceSpm: Int? = null,
    val avgSpeedMs: Float? = null,
    val lat: Double? = null,
    val lon: Double? = null,
    val weather: WorkoutWeatherInfo? = null,
    val note: String = "",
    val photoUri: String? = null,
    /** Всегда верх кадра (0). Поле оставлено для совместимости сохранённых заметок. */
    val photoFocusY: Float = 0f,
    val mapPoints: Int = 0,
    val isNew: Boolean = true,
)

@Composable
fun WorkoutSummaryScreen(
    data: WorkoutSummaryData,
    isSaving: Boolean,
    onBack: () -> Unit,
    onSave: (WorkoutSummaryData) -> Unit,
) {
    val context = LocalContext.current
    var note by remember(data.note) { mutableStateOf(data.note) }
    var photoUri by remember(data.photoUri) { mutableStateOf(data.photoUri) }
    var weather by remember(data.weather) { mutableStateOf(data.weather) }
    var weatherLoading by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) photoUri = uri.toString()
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture(),
    ) { ok ->
        val uri = pendingCameraUri
        pendingCameraUri = null
        if (ok && uri != null) photoUri = uri.toString()
    }

    LaunchedEffect(data.lat, data.lon, data.weather) {
        if (data.weather != null || data.lat == null || data.lon == null) return@LaunchedEffect
        weatherLoading = true
        weather = WorkoutWeatherFetcher.fetch(data.lat, data.lon)
        weatherLoading = false
    }

    val zone = ZoneId.systemDefault()
    val started = Instant.ofEpochMilli(data.startEpochMs).atZone(zone)
    val dateText = started.format(DateTimeFormatter.ofPattern("d MMMM yyyy"))
    val timeText = started.format(DateTimeFormatter.ofPattern("HH:mm"))

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState()),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(413.dp)
                .background(Brush.linearGradient(brandingGradient())),
        ) {
            if (photoUri != null) {
                AsyncImage(
                    model = photoUri,
                    contentDescription = "Обложка тренировки",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    alignment = Alignment.TopCenter,
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.28f)),
                )
            }
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
                    .padding(8.dp),
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Назад",
                    tint = MaterialTheme.colorScheme.onPrimary,
                )
            }
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(8.dp),
            ) {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(
                        Icons.Filled.MoreVert,
                        contentDescription = "Меню",
                        tint = MaterialTheme.colorScheme.onPrimary,
                    )
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                ) {
                    DropdownMenuItem(
                        text = { Text("Выбрать из галереи") },
                        onClick = {
                            menuExpanded = false
                            galleryLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                            )
                        },
                        leadingIcon = {
                            Icon(Icons.Filled.PhotoLibrary, contentDescription = null)
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Сделать снимок") },
                        onClick = {
                            menuExpanded = false
                            val uri = createSocialCameraUri(context, "workout_cover")
                            pendingCameraUri = uri
                            cameraLauncher.launch(uri)
                        },
                        leadingIcon = {
                            Icon(Icons.Filled.PhotoCamera, contentDescription = null)
                        },
                    )
                }
            }
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = data.activityTitleRu,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
                Text(
                    text = "$dateText · $timeText",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f),
                )
            }
        }

        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AppCard {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Сводка",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = contentPrimaryColor(),
                    )
                    SummaryMetricRow(
                        icon = Icons.Filled.LocalFireDepartment,
                        label = "Калории",
                        value = "${data.calories.toInt()} ккал",
                    )
                    SummaryMetricRow(
                        icon = Icons.Filled.Timer,
                        label = "Общее время",
                        value = "${data.durationMinutes} мин",
                    )
                    SummaryMetricRow(
                        icon = Icons.Filled.Straighten,
                        label = "Дистанция",
                        value = "%.2f км".format(data.distanceKm),
                    )
                    SummaryMetricRow(
                        icon = Icons.Filled.DirectionsRun,
                        label = "Вид деятельности",
                        value = data.activityTitleRu,
                    )
                    data.avgPaceMinPerKm?.let {
                        SummaryMetricRow(
                            icon = Icons.Filled.Timer,
                            label = "Средний темп",
                            value = formatPaceSummary(it),
                        )
                    }
                }
            }

            AppCard {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Погода",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = contentPrimaryColor(),
                    )
                    when {
                        weatherLoading -> {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                Text(
                                    text = "Загружаем погоду…",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        weather != null -> {
                            val w = weather!!
                            SummaryMetricRow(
                                icon = Icons.Filled.Thermostat,
                                label = w.description ?: "Температура",
                                value = w.temperatureC?.let { "%.0f °C".format(it) } ?: "—",
                            )
                            SummaryMetricRow(
                                icon = Icons.Filled.WaterDrop,
                                label = "Влажность",
                                value = w.humidityPercent?.let { "$it %" } ?: "—",
                            )
                            SummaryMetricRow(
                                icon = Icons.Filled.Air,
                                label = "Ветер",
                                value = w.windKmh?.let { "%.0f км/ч".format(it) } ?: "—",
                            )
                        }
                        else -> {
                            Text(
                                text = "Нет данных о погоде",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            AppCard {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Заметка",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = contentPrimaryColor(),
                    )
                    AppTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = "Как прошла тренировка?",
                        singleLine = false,
                    )
                }
            }

            AppButton(
                text = if (isSaving) {
                    "Сохраняем…"
                } else if (data.isNew) {
                    "Сохранить и к активности"
                } else {
                    "Обновить и назад"
                },
                onClick = {
                    onSave(
                        data.copy(
                            note = note,
                            photoUri = photoUri,
                            photoFocusY = 0f,
                            weather = weather,
                        ),
                    )
                },
                enabled = !isSaving,
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

fun buildWorkoutNotes(data: WorkoutSummaryData): String {
    return WorkoutNotesCodec.encode(
        meta = WorkoutSessionMeta(
            weather = data.weather,
            photoUri = data.photoUri,
            photoFocusY = 0f,
            avgPaceMinPerKm = data.avgPaceMinPerKm,
            cadenceSpm = data.cadenceSpm,
            mapPoints = data.mapPoints,
        ),
        note = data.note,
    )
}

@Composable
private fun SummaryMetricRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        }
    }
}

private fun formatPaceSummary(minPerKm: Double): String {
    if (minPerKm.isNaN() || minPerKm > 30.0) return "—"
    val minutes = minPerKm.toInt()
    val seconds = ((minPerKm - minutes) * 60).toInt().coerceIn(0, 59)
    return "%d:%02d /км".format(minutes, seconds)
}
