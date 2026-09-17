package com.example.healtapp.features.activity.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.healtapp.core.ui.components.AppButton
import com.example.healtapp.core.ui.theme.brandingGradient
import com.example.healtapp.core.ui.theme.contentPrimaryColor
import com.example.healtapp.features.activity.live.LiveWorkoutService
import com.example.healtapp.features.activity.live.LiveWorkoutSession
import com.example.healtapp.features.activity.live.LiveWorkoutSnapshot
import com.example.healtapp.features.activity.live.RunGoal
import com.example.healtapp.features.activity.live.isCompleted
import com.example.healtapp.features.activity.live.progressFraction
import com.example.healtapp.features.activity.live.titleRu
import com.example.healtapp.features.activity.presentation.activityApiSlug
import com.example.healtapp.features.activity.presentation.estimateTrainingCalories
import com.example.healtapp.features.activity.ui.components.LiveWorkoutMap
import com.example.healtapp.features.activity.ui.components.trainingIconForSlug
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged

private enum class RunPhase { Prep, Countdown, Active }

@Composable
fun RunWorkoutScreen(
    activityTitleRu: String,
    session: LiveWorkoutSession,
    previousRuns: List<PreviousRunOption>,
    bedtimeWarning: String?,
    onBack: () -> Unit,
    onFinished: (LiveWorkoutSnapshot) -> Unit,
) {
    val context = LocalContext.current
    val snapshot by session.snapshot.collectAsStateWithLifecycle()
    var phase by remember {
        mutableStateOf(if (snapshot.isActive) RunPhase.Active else RunPhase.Prep)
    }
    var selectedGoal by remember { mutableStateOf<RunGoal>(RunGoal.Free) }
    var distanceWhole by remember { mutableIntStateOf(2) }
    var distanceFrac by remember { mutableIntStateOf(5) }
    var caloriesValue by remember { mutableIntStateOf(300) }
    var pauseOnGoal by remember { mutableStateOf(true) }
    var previewLat by remember { mutableStateOf<Double?>(null) }
    var previewLon by remember { mutableStateOf<Double?>(null) }
    var previewHeading by remember { mutableStateOf<Float?>(null) }
    var permissionDenied by remember { mutableStateOf(false) }
    var countdown by remember { mutableIntStateOf(3) }
    var goalHandled by remember { mutableStateOf(false) }
    var pendingStart by remember { mutableStateOf(false) }

    val permissions = remember {
        buildList {
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            add(Manifest.permission.ACCESS_COARSE_LOCATION)
            if (Build.VERSION.SDK_INT >= 29) add(Manifest.permission.ACTIVITY_RECOGNITION)
            if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
        }.toTypedArray()
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        val granted = result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        permissionDenied = !granted
        if (granted && pendingStart) {
            pendingStart = false
            countdown = 3
            phase = RunPhase.Countdown
        }
    }

    fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    fun resolveGoal(): RunGoal = when (selectedGoal) {
        is RunGoal.DistanceKm ->
            RunGoal.DistanceKm((distanceWhole + distanceFrac / 10f).coerceAtLeast(0.1f))
        is RunGoal.Calories ->
            RunGoal.Calories(caloriesValue.coerceAtLeast(50))
        else -> selectedGoal
    }

    fun beginCountdown() {
        selectedGoal = resolveGoal()
        goalHandled = false
        if (!hasLocationPermission()) {
            pendingStart = true
            permissionLauncher.launch(permissions)
            return
        }
        countdown = 3
        phase = RunPhase.Countdown
    }

    fun startLiveService() {
        phase = RunPhase.Active
        LiveWorkoutService.start(context, activityTitleRu)
    }

    LaunchedEffect(Unit) {
        if (!hasLocationPermission()) {
            permissionLauncher.launch(permissions)
        }
    }

    LaunchedEffect(phase) {
        if (phase != RunPhase.Countdown) return@LaunchedEffect
        for (n in 3 downTo 1) {
            countdown = n
            delay(1000L)
        }
        countdown = 0
        delay(280L)
        startLiveService()
    }

    DisposableEffect(phase) {
        if (phase != RunPhase.Prep) {
            onDispose { }
        } else {
            val fused = LocationServices.getFusedLocationProviderClient(context)
            if (!hasLocationPermission()) {
                onDispose { }
            } else {
                fused.lastLocation.addOnSuccessListener { loc ->
                    if (loc != null) {
                        previewLat = loc.latitude
                        previewLon = loc.longitude
                        if (loc.hasBearing()) previewHeading = loc.bearing
                    }
                }
                val callback = object : LocationCallback() {
                    override fun onLocationResult(result: LocationResult) {
                        val loc = result.lastLocation ?: return
                        previewLat = loc.latitude
                        previewLon = loc.longitude
                        if (loc.hasBearing()) previewHeading = loc.bearing
                    }
                }
                val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000L)
                    .setMinUpdateIntervalMillis(1000L)
                    .build()
                runCatching {
                    fused.requestLocationUpdates(request, callback, Looper.getMainLooper())
                }
                onDispose {
                    runCatching { fused.removeLocationUpdates(callback) }
                }
            }
        }
    }

    val calories = liveWorkoutCalories(snapshot)
    val goalDone = phase == RunPhase.Active &&
        selectedGoal !is RunGoal.Free &&
        selectedGoal.isCompleted(snapshot.distanceKm, calories)

    LaunchedEffect(goalDone, pauseOnGoal) {
        if (!goalDone || goalHandled || !snapshot.isActive) return@LaunchedEffect
        goalHandled = true
        if (pauseOnGoal) {
            if (!snapshot.isPaused) LiveWorkoutService.pause(context)
        } else {
            val done = session.stop()
            LiveWorkoutService.stop(context)
            onFinished(done)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LiveWorkoutMap(
            points = if (phase == RunPhase.Active) snapshot.points else emptyList(),
            headingDegrees = if (phase == RunPhase.Active) snapshot.headingDegrees else previewHeading,
            previewLat = if (phase == RunPhase.Active) snapshot.lastLat else previewLat,
            previewLon = if (phase == RunPhase.Active) snapshot.lastLon else previewLon,
            followUser = true,
            modifier = Modifier.fillMaxSize(),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = {
                    if (snapshot.isActive || phase == RunPhase.Countdown) {
                        session.discard()
                        LiveWorkoutService.stop(context)
                    }
                    onBack()
                },
                modifier = Modifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)),
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
            }
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    trainingIconForSlug(activityApiSlug(activityTitleRu)),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = activityTitleRu,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(modifier = Modifier.size(48.dp))
        }

        if (phase == RunPhase.Countdown) {
            CountdownOverlay(value = countdown)
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(12.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.96f))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            when (phase) {
                RunPhase.Prep -> PrepPanel(
                    selectedGoal = selectedGoal,
                    distanceWhole = distanceWhole,
                    distanceFrac = distanceFrac,
                    caloriesValue = caloriesValue,
                    pauseOnGoal = pauseOnGoal,
                    previousRuns = previousRuns,
                    bedtimeWarning = bedtimeWarning,
                    permissionDenied = permissionDenied,
                    onSelectGoal = { selectedGoal = it },
                    onDistanceWhole = { distanceWhole = it },
                    onDistanceFrac = { distanceFrac = it },
                    onCalories = { caloriesValue = it },
                    onPauseOnGoal = { pauseOnGoal = it },
                    onStart = { beginCountdown() },
                )
                RunPhase.Countdown -> {
                    Text(
                        text = "Готовимся…",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "Цель: ${resolveGoal().titleRu()}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                RunPhase.Active -> ActivePanel(
                    snapshot = snapshot,
                    selectedGoal = selectedGoal,
                    calories = calories,
                    onPauseToggle = {
                        if (snapshot.isPaused) LiveWorkoutService.resume(context)
                        else LiveWorkoutService.pause(context)
                    },
                    onFinish = {
                        val done = session.stop()
                        LiveWorkoutService.stop(context)
                        onFinished(done)
                    },
                )
            }
        }
    }
}

@Composable
private fun CountdownOverlay(value: Int) {
    val scale = remember { Animatable(0.6f) }
    LaunchedEffect(value) {
        scale.snapTo(0.55f)
        scale.animateTo(1.15f, tween(420))
        scale.animateTo(1f, tween(180))
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.35f)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = if (value > 0) value.toString() else "Вперёд!",
            modifier = Modifier.graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            },
            style = MaterialTheme.typography.displayLarge.copy(fontSize = 96.sp),
            fontWeight = FontWeight.Bold,
            color = androidx.compose.ui.graphics.Color.White,
        )
    }
}

@Composable
private fun PrepPanel(
    selectedGoal: RunGoal,
    distanceWhole: Int,
    distanceFrac: Int,
    caloriesValue: Int,
    pauseOnGoal: Boolean,
    previousRuns: List<PreviousRunOption>,
    bedtimeWarning: String?,
    permissionDenied: Boolean,
    onSelectGoal: (RunGoal) -> Unit,
    onDistanceWhole: (Int) -> Unit,
    onDistanceFrac: (Int) -> Unit,
    onCalories: (Int) -> Unit,
    onPauseOnGoal: (Boolean) -> Unit,
    onStart: () -> Unit,
) {
    Text(
        text = "Цель тренировки",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = contentPrimaryColor(),
    )
    bedtimeWarning?.let {
        Text(text = it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
    }
    if (permissionDenied) {
        Text(
            text = "Нужен доступ к геолокации, чтобы строить маршрут",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
        )
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        GoalChip(
            title = "Без цели",
            selected = selectedGoal is RunGoal.Free,
            onClick = { onSelectGoal(RunGoal.Free) },
            modifier = Modifier.weight(1f),
        )
        GoalChip(
            title = "Дистанция",
            selected = selectedGoal is RunGoal.DistanceKm,
            onClick = {
                onSelectGoal(RunGoal.DistanceKm(distanceWhole + distanceFrac / 10f))
            },
            modifier = Modifier.weight(1f),
        )
        GoalChip(
            title = "Ккал",
            selected = selectedGoal is RunGoal.Calories,
            onClick = { onSelectGoal(RunGoal.Calories(caloriesValue)) },
            modifier = Modifier.weight(1f),
        )
    }
    if (selectedGoal is RunGoal.DistanceKm) {
        DistanceWheels(
            whole = distanceWhole,
            frac = distanceFrac,
            onWhole = onDistanceWhole,
            onFrac = onDistanceFrac,
        )
    }
    if (selectedGoal is RunGoal.Calories) {
        CaloriesWheel(value = caloriesValue, onValue = onCalories)
    }
    previousRuns.take(2).forEach { prev ->
        GoalChip(
            title = "Обойти: ${prev.label}",
            selected = selectedGoal is RunGoal.BeatPrevious &&
                (selectedGoal as RunGoal.BeatPrevious).label == prev.label,
            onClick = {
                onSelectGoal(
                    RunGoal.BeatPrevious(
                        previousDistanceKm = prev.distanceKm,
                        previousDurationMin = prev.durationMin,
                        label = prev.label,
                    ),
                )
            },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = true,
        )
    }
    if (selectedGoal !is RunGoal.Free) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Пауза при достижении цели",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            Switch(checked = pauseOnGoal, onCheckedChange = onPauseOnGoal)
        }
    }
    AppButton(text = "Начать", onClick = onStart)
}

@Composable
private fun ActivePanel(
    snapshot: LiveWorkoutSnapshot,
    selectedGoal: RunGoal,
    calories: Float,
    onPauseToggle: () -> Unit,
    onFinish: () -> Unit,
) {
    val zone = ZoneId.systemDefault()
    val started = if (snapshot.startedAtEpochMs > 0L) {
        Instant.ofEpochMilli(snapshot.startedAtEpochMs).atZone(zone)
    } else {
        null
    }
    val dateText = started?.format(DateTimeFormatter.ofPattern("d MMM yyyy")) ?: "—"
    val timeText = started?.format(DateTimeFormatter.ofPattern("HH:mm")) ?: "—"

    Text(
        text = formatElapsed(snapshot.movingMs.takeIf { it > 0L } ?: snapshot.elapsedMs),
        style = MaterialTheme.typography.displaySmall.copy(fontSize = 42.sp),
        fontWeight = FontWeight.Bold,
        color = contentPrimaryColor(),
    )
    Text(
        text = "$dateText · $timeText",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    snapshot.statusMessage?.let {
        Text(
            text = it,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        MetricTile(
            icon = Icons.Filled.Straighten,
            label = "Расстояние",
            value = "%.2f км".format(snapshot.distanceKm),
            modifier = Modifier.weight(1f),
        )
        MetricTile(
            icon = Icons.Filled.Timer,
            label = "Темп/км",
            value = formatPace(snapshot.currentPaceMinPerKm ?: snapshot.avgPaceMinPerKm),
            modifier = Modifier.weight(1f),
        )
        MetricTile(
            icon = Icons.Filled.Speed,
            label = "Шаг/м",
            value = snapshot.cadenceSpm?.toString() ?: "—",
            modifier = Modifier.weight(1f),
        )
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        MetricTile(
            icon = Icons.Filled.LocalFireDepartment,
            label = "Ккал",
            value = calories.toInt().toString(),
            modifier = Modifier.weight(1f),
        )
        MetricTile(
            icon = Icons.Filled.DirectionsRun,
            label = "Длительность",
            value = formatElapsed(snapshot.movingMs.takeIf { it > 0L } ?: snapshot.elapsedMs),
            modifier = Modifier.weight(1f),
        )
    }
    if (selectedGoal !is RunGoal.Free) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "Цель: ${selectedGoal.titleRu()}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            LinearProgressIndicator(
                progress = { selectedGoal.progressFraction(snapshot.distanceKm, calories) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(8.dp)),
            )
        }
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        AppButton(
            text = if (snapshot.isPaused) "Продолжить" else "Пауза",
            onClick = onPauseToggle,
            isSecondary = true,
            enabled = snapshot.isActive,
            modifier = Modifier.weight(1f),
        )
        AppButton(
            text = "Завершить",
            onClick = onFinish,
            enabled = snapshot.isActive,
            modifier = Modifier.weight(1f),
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DistanceWheels(
    whole: Int,
    frac: Int,
    onWhole: (Int) -> Unit,
    onFrac: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "%.1f км".format(whole + frac / 10f),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Brush.linearGradient(brandingGradient().map { it.copy(alpha = 0.12f) })),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            NumberWheel(
                values = (0..42).toList(),
                selected = whole.coerceIn(0, 42),
                onSelected = onWhole,
                modifier = Modifier.width(88.dp).fillMaxHeight(),
            )
            Text(
                text = ".",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            NumberWheel(
                values = (0..9).toList(),
                selected = frac.coerceIn(0, 9),
                onSelected = onFrac,
                modifier = Modifier.width(72.dp).fillMaxHeight(),
            )
            Text(
                text = "км",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CaloriesWheel(
    value: Int,
    onValue: (Int) -> Unit,
) {
    val options = remember { (50..2000 step 50).toList() }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "$value ккал",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Brush.linearGradient(brandingGradient().map { it.copy(alpha = 0.12f) })),
            contentAlignment = Alignment.Center,
        ) {
            NumberWheel(
                values = options,
                selected = options.minByOrNull { kotlin.math.abs(it - value) } ?: 300,
                onSelected = onValue,
                modifier = Modifier.width(120.dp).fillMaxHeight(),
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NumberWheel(
    values: List<Int>,
    selected: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val itemHeight = 44.dp
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = values.indexOf(selected).coerceAtLeast(0),
    )
    val fling = rememberSnapFlingBehavior(lazyListState = listState)

    LaunchedEffect(listState) {
        snapshotFlow {
            val layout = listState.layoutInfo
            val center = (layout.viewportStartOffset + layout.viewportEndOffset) / 2
            layout.visibleItemsInfo.minByOrNull {
                kotlin.math.abs((it.offset + it.size / 2) - center)
            }?.index
        }
            .distinctUntilChanged()
            .collect { index ->
                if (index != null && index in values.indices) {
                    onSelected(values[index])
                }
            }
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(itemHeight)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)),
        )
        LazyColumn(
            state = listState,
            flingBehavior = fling,
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 48.dp),
        ) {
            itemsIndexed(values) { _, value ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(itemHeight),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = value.toString(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = if (value == selected) FontWeight.Bold else FontWeight.Medium,
                        color = if (value == selected) {
                            contentPrimaryColor()
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            }
        }
    }
}

data class PreviousRunOption(
    val distanceKm: Float,
    val durationMin: Int,
    val label: String,
)

@Composable
private fun GoalChip(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: Boolean = false,
) {
    val border = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outline.copy(alpha = forAlphaBorder(selected))
    }
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, border, RoundedCornerShape(16.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                else MaterialTheme.colorScheme.surface,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leadingIcon) {
            Icon(Icons.Filled.Flag, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
        }
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = contentPrimaryColor(),
        )
    }
}

private fun forAlphaBorder(selected: Boolean): Float = if (selected) 1f else 0.25f

@Composable
private fun MetricTile(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.linearGradient(brandingGradient().map { it.copy(alpha = 0.16f) }))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
        Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

fun liveWorkoutCalories(snapshot: LiveWorkoutSnapshot): Float {
    return estimateTrainingCalories(
        snapshot.activityTitleRu,
        snapshot.durationMinutes,
        snapshot.distanceKm.toFloat(),
    )
}

private fun formatElapsed(ms: Long): String {
    val totalSec = (ms / 1000L).coerceAtLeast(0L)
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}

private fun formatPace(minPerKm: Double?): String {
    if (minPerKm == null || minPerKm.isNaN() || minPerKm > 30.0) return "—"
    val minutes = minPerKm.toInt()
    val seconds = ((minPerKm - minutes) * 60).toInt().coerceIn(0, 59)
    return "%d:%02d".format(minutes, seconds)
}
