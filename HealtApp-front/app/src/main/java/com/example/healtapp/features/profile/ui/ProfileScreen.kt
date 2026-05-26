package com.example.healtapp.features.profile.ui

import android.content.pm.PackageManager
import android.Manifest
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.healtapp.BuildConfig
import com.example.healtapp.core.common.Constants
import com.example.healtapp.core.ui.components.AppButton
import com.example.healtapp.core.ui.components.AppMessageBanner
import com.example.healtapp.core.ui.components.AppMessageType
import com.example.healtapp.core.ui.components.AppCard
import com.example.healtapp.core.ui.components.AppScreen
import com.example.healtapp.core.ui.components.AppTextField
import com.example.healtapp.core.ui.components.DatePickerField
import com.example.healtapp.core.ui.components.SectionHeader
import com.example.healtapp.core.ui.theme.chipSelectedColor
import com.example.healtapp.core.ui.theme.metricIconGradient
import com.example.healtapp.core.ui.theme.themedCardBlue
import com.example.healtapp.core.ui.theme.themedCardMint
import com.example.healtapp.di.ApiServerConfigEntryPoint
import com.example.healtapp.di.ImageLoaderEntryPoint
import com.example.healtapp.features.auth.ui.components.ChangePasswordForm
import com.example.healtapp.features.auth.ui.components.ChangePasswordFormHeader
import com.example.healtapp.features.onboarding.ui.components.ActivityLevelSelector
import com.example.healtapp.features.onboarding.ui.components.GoalSelector
import com.example.healtapp.features.profile.presentation.ProfileEditViewModel
import com.example.healtapp.features.profile.ui.components.ProfileBodyStatsCard
import com.example.healtapp.features.profile.ui.components.ProfileGoalsEditSheet
import com.example.healtapp.features.profile.ui.components.ProfileGoalsStrip
import com.example.healtapp.features.profile.ui.components.ProfileHeaderAvatar
import com.example.healtapp.features.profile.ui.components.ProfileNavLink
import com.example.healtapp.features.profile.ui.components.ProfileThemeSelector
import dagger.hilt.android.EntryPointAccessors
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onOpenDataPrivacy: () -> Unit = {},
    onOpenIntegrations: () -> Unit = {},
    onOpenMiBandBle: () -> Unit = {},
    onOpenServerConnection: () -> Unit = {},
    onOpenNotifications: () -> Unit = {},
    onOpenAchievements: () -> Unit = {},
    onOpenFriends: () -> Unit = {},
    onOpenNutritionGuide: () -> Unit = {},
    onLogout: () -> Unit = {},
) {
    val viewModel: ProfileEditViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val imageLoader = remember(context) {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            ImageLoaderEntryPoint::class.java,
        ).imageLoader()
    }

    val initial = when {
        uiState.goal.isNotBlank() -> uiState.goal.first().uppercaseChar().toString()
        uiState.age.isNotBlank() -> uiState.age.first().toString()
        else -> "Я"
    }

    var showAvatarSheet by remember { mutableStateOf(false) }
    var showGoalsSheet by remember { mutableStateOf(false) }
    var goalsCardExpanded by remember { mutableStateOf(false) }
    var basicsCardExpanded by remember { mutableStateOf(false) }
    var dietCardExpanded by remember { mutableStateOf(false) }
    var goalsSavePending by remember { mutableStateOf(false) }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }

    LaunchedEffect(uiState.isSaving, uiState.success) {
        if (goalsSavePending && !uiState.isSaving && uiState.success != null) {
            showGoalsSheet = false
            goalsSavePending = false
        }
    }

    val pickImage = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri -> uri?.let { viewModel.uploadAvatarFromUri(it) } }

    val takePicture = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture(),
    ) { success ->
        val uri = pendingCameraUri
        pendingCameraUri = null
        if (success && uri != null) viewModel.uploadAvatarFromUri(uri)
    }

    val requestCameraPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) launchCamera(context) { uri ->
            pendingCameraUri = uri
            takePicture.launch(uri)
        }
    }

    val apiServerConfig = remember {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            ApiServerConfigEntryPoint::class.java,
        ).apiServerConfig()
    }
    val avatarUrl = remember(uiState.hasAvatar, uiState.avatarLoadNonce, apiServerConfig.baseUrl()) {
        if (!uiState.hasAvatar) null
        else "${apiServerConfig.avatarBase()}/profile/me/avatar?v=${uiState.avatarLoadNonce}"
    }

    if (showAvatarSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showAvatarSheet = false },
            sheetState = sheetState,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = "Фото профиля",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(8.dp))
                AvatarSheetRow(Icons.Outlined.PhotoLibrary, "Галерея") {
                    showAvatarSheet = false
                    pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }
                AvatarSheetRow(Icons.Outlined.PhotoCamera, "Сделать фото") {
                    showAvatarSheet = false
                    when {
                        ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                            PackageManager.PERMISSION_GRANTED -> {
                            launchCamera(context) { uri ->
                                pendingCameraUri = uri
                                takePicture.launch(uri)
                            }
                        }
                        else -> requestCameraPermission.launch(Manifest.permission.CAMERA)
                    }
                }
                if (uiState.hasAvatar) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    AvatarSheetRow(
                        Icons.Outlined.DeleteOutline,
                        "Удалить фото",
                        iconTint = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.error,
                    ) {
                        showAvatarSheet = false
                        viewModel.deleteAvatar()
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }

    val avatarEnabled = !uiState.isLoading && !uiState.isUploadingAvatar && !uiState.guestMode

    AppScreen(
        title = "Профиль",
        subtitle = "Данные · цели · настройки",
        headerLeading = {
            ProfileHeaderAvatar(
                initial = initial,
                avatarUrl = avatarUrl,
                imageLoader = imageLoader,
                isUploading = uiState.isUploadingAvatar,
            )
        },
        onHeaderLeadingClick = if (avatarEnabled) {
            { showAvatarSheet = true }
        } else {
            null
        },
        scrollable = true,
    ) {
        if (!uiState.guestMode) {
            ProfileGoalsStrip(
                targetSleep = uiState.targetSleep,
                targetWater = uiState.targetWater,
                targetSteps = uiState.targetSteps,
                onEditClick = { showGoalsSheet = true },
            )

            ProfileBodyStatsCard(
                heightCm = uiState.height,
                weightKg = uiState.weight,
                weightHistory = uiState.weightHistory,
                weightWeeklyReminder = uiState.weightWeeklyReminder,
            )
        }

        ProfileGoalsEditSheet(
            visible = showGoalsSheet,
            targetSleep = uiState.targetSleep,
            targetWater = uiState.targetWater,
            targetSteps = uiState.targetSteps,
            isSaving = uiState.isSaving,
            guestMode = uiState.guestMode,
            onDismiss = { showGoalsSheet = false },
            onSleepChange = viewModel::updateTargetSleep,
            onWaterChange = viewModel::updateTargetWater,
            onStepsChange = viewModel::updateTargetSteps,
            onSave = {
                goalsSavePending = true
                viewModel.save()
            },
        )

        if (uiState.guestMode) {
            AppCard {
                Text(
                    text = "Локальный режим: разделы открыты, но API без входа недоступен. Выйди и войди с email для синхронизации.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        uiState.error?.let {
            AppMessageBanner(text = it, type = AppMessageType.Error)
        }
        uiState.success?.let {
            AppMessageBanner(text = it, type = AppMessageType.Success)
        }

        ProfileExpandableCard(
            title = "Оформление",
            icon = Icons.Filled.Palette,
            initiallyExpanded = false,
        ) {
            ProfileThemeSelector(
                selected = uiState.themeMode,
                onSelected = viewModel::setThemeMode,
            )
        }

        ProfileExpandableCard(
            title = "Питание и ограничения",
            icon = Icons.Filled.Restaurant,
            initiallyExpanded = false,
            expanded = dietCardExpanded,
            onExpandedChange = { dietCardExpanded = it },
        ) {
            Text(text = "Вегетарианец?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FilterChip(
                    selected = !uiState.isVegetarian,
                    onClick = { viewModel.updateIsVegetarian(false) },
                    label = { Text("Нет") },
                )
                FilterChip(
                    selected = uiState.isVegetarian,
                    onClick = { viewModel.updateIsVegetarian(true) },
                    label = { Text("Да") },
                    leadingIcon = { Icon(Icons.Filled.Eco, contentDescription = null) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = chipSelectedColor(themedCardMint()),
                    ),
                )
            }
            Text(text = "Аллергии?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FilterChip(
                    selected = !uiState.hasAllergies,
                    onClick = { viewModel.updateHasAllergies(false) },
                    label = { Text("Нет") },
                )
                FilterChip(
                    selected = uiState.hasAllergies,
                    onClick = { viewModel.updateHasAllergies(true) },
                    label = { Text("Да") },
                )
            }
            if (uiState.hasAllergies) {
                AppTextField(
                    uiState.allergiesText,
                    viewModel::updateAllergiesText,
                    label = "На что аллергия",
                )
            }
            AppButton(
                text = if (uiState.isSaving) "Сохраняем..." else "Сохранить",
                enabled = !uiState.isSaving && !uiState.isLoading && !uiState.guestMode,
                onClick = viewModel::save,
            )
        }

        ProfileExpandableCard(
            title = "Основные данные",
            icon = Icons.Filled.Tune,
            initiallyExpanded = false,
            expanded = basicsCardExpanded,
            onExpandedChange = { basicsCardExpanded = it },
        ) {
            DatePickerField(
                value = uiState.birthDate,
                onValueChange = viewModel::updateBirthDate,
                label = "Дата рождения",
                enabled = !uiState.guestMode,
            )
            if (uiState.age.isNotBlank()) {
                Text(
                    text = "Возраст: ${uiState.age} лет (обновляется автоматически)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            AppTextField(uiState.height, viewModel::updateHeight, label = "Рост (см)")
            AppTextField(uiState.weight, viewModel::updateWeight, label = "Вес (кг)")
            AppButton(
                text = when {
                    uiState.isSaving -> "Сохраняем..."
                    uiState.isLoading -> "Загрузка..."
                    else -> "Сохранить данные"
                },
                enabled = !uiState.isSaving && !uiState.isLoading && !uiState.guestMode,
                onClick = viewModel::save,
            )
            Text(
                text = "Пол",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FilterChip(
                    selected = uiState.sex == Constants.Sex.MALE,
                    onClick = { viewModel.updateSex(Constants.Sex.MALE) },
                    label = { Text("Мужской") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = chipSelectedColor(themedCardBlue()),
                    ),
                )
                FilterChip(
                    selected = uiState.sex == Constants.Sex.FEMALE,
                    onClick = { viewModel.updateSex(Constants.Sex.FEMALE) },
                    label = { Text("Женский") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = chipSelectedColor(themedCardMint()),
                    ),
                )
            }
        }

        ProfileExpandableCard(
            title = "Цели и привычки",
            icon = Icons.Filled.Flag,
            initiallyExpanded = false,
            expanded = goalsCardExpanded,
            onExpandedChange = { goalsCardExpanded = it },
        ) {
            Text(
                text = "Дневные ориентиры",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "Калории и БЖУ настраиваются в разделе «Питание»",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            AppTextField(uiState.targetSleep, viewModel::updateTargetSleep, label = "Цель сна (часы)")
            AppTextField(uiState.targetWater, viewModel::updateTargetWater, label = "Цель воды (мл)")
            AppTextField(uiState.targetSteps, viewModel::updateTargetSteps, label = "Цель шагов в день")

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

            Text(
                text = "Мотивация и активность",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            GoalSelector(
                selectedGoal = uiState.goal.ifBlank { Constants.Goals.IMPROVE_ENERGY },
                onGoalSelected = viewModel::updateGoal,
            )
            ActivityLevelSelector(
                selected = uiState.activityLevel.ifBlank { Constants.ActivityLevel.MEDIUM },
                onSelected = viewModel::updateActivityLevel,
            )
            AppButton(
                text = when {
                    uiState.isSaving -> "Сохраняем..."
                    uiState.isLoading -> "Загрузка..."
                    else -> "Сохранить цели и привычки"
                },
                enabled = !uiState.isSaving && !uiState.isLoading && !uiState.isChangingPassword && !uiState.guestMode,
                onClick = viewModel::save,
            )
        }

        ProfileExpandableCard(
            title = "Пароль",
            icon = Icons.Filled.Lock,
            initiallyExpanded = false,
        ) {
            ChangePasswordFormHeader()
            ChangePasswordForm(
                currentPassword = uiState.currentPassword,
                newPassword = uiState.newPassword,
                confirmPassword = uiState.confirmPassword,
                isChanging = uiState.isChangingPassword,
                enabled = !uiState.isLoading && !uiState.isSaving && !uiState.guestMode,
                error = null,
                success = null,
                onCurrentChange = viewModel::updateCurrentPassword,
                onNewChange = viewModel::updateNewPassword,
                onConfirmChange = viewModel::updateConfirmPassword,
                onSubmit = viewModel::changeAccountPassword,
            )
        }

        SectionHeader(
            title = "Сообщество",
            subtitle = "Достижения, друзья и лента",
        )

        ProfileCommunityHubCard(
            onOpenAchievements = onOpenAchievements,
            onOpenFriends = onOpenFriends,
        )

        SectionHeader(
            title = "Сервис",
            subtitle = "Данные, интеграции и выход",
        )

        AppCard {
            Column {
                ProfileNavLink(
                    icon = Icons.Filled.Description,
                    title = "Конфиденциальность",
                    subtitle = "Данные, аккаунт и удаление",
                    onClick = onOpenDataPrivacy,
                    showDivider = false,
                )
                ProfileNavLink(
                    icon = Icons.Filled.Link,
                    title = "Интеграции",
                    subtitle = "Health Connect, FatSecret, Mi Band BLE",
                    onClick = onOpenIntegrations,
                )
                ProfileNavLink(
                    icon = Icons.Filled.Watch,
                    title = "Mi Band 8 (BLE)",
                    subtitle = "Прямое подключение Xiaomi",
                    onClick = onOpenMiBandBle,
                )
                ProfileNavLink(
                    icon = Icons.Filled.Notifications,
                    title = "Уведомления",
                    subtitle = "Вода, еда, советы",
                    onClick = onOpenNotifications,
                )
                ProfileNavLink(
                    icon = Icons.Filled.Restaurant,
                    title = "Подсказки: дневник питания",
                    subtitle = "Показать обучение снова",
                    onClick = onOpenNutritionGuide,
                    showDivider = false,
                )
            }
        }

        AppCard {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(
                        Icons.Filled.Logout,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                    )
                    Text(
                        text = "Сессия",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                AppButton(
                    text = "Выйти из аккаунта",
                    onClick = { viewModel.logout(onLogout) },
                    isSecondary = true,
                    enabled = !uiState.isLoading && !uiState.isSaving && !uiState.isUploadingAvatar && !uiState.isChangingPassword,
                )
            }
        }

        Spacer(Modifier.height(72.dp))
    }
}

private fun launchCamera(context: android.content.Context, onUri: (Uri) -> Unit) {
    val dir = File(context.cacheDir, "camera").apply { mkdirs() }
    val file = File(dir, "avatar_${System.currentTimeMillis()}.jpg")
    val uri = FileProvider.getUriForFile(
        context,
        "${BuildConfig.APPLICATION_ID}.fileprovider",
        file,
    )
    onUri(uri)
}

@Composable
private fun ProfileCommunityHubCard(
    onOpenAchievements: () -> Unit,
    onOpenFriends: () -> Unit,
) {
    AppCard {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Brush.linearGradient(metricIconGradient(themedCardMint()))),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Group,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Ваше сообщество",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "Награды, рекорды и друзья в одном месте",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CommunityActionTile(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Filled.EmojiEvents,
                    title = "Достижения",
                    subtitle = "Серии, цели и рекорды",
                    onClick = onOpenAchievements,
                    accent = themedCardMint(),
                )
                CommunityActionTile(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Filled.Group,
                    title = "Друзья",
                    subtitle = "Лента и приватность",
                    onClick = onOpenFriends,
                    accent = themedCardBlue(),
                )
            }
        }
    }
}

@Composable
private fun CommunityActionTile(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    accent: Color,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(metricIconGradient(accent)))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(26.dp),
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun AvatarSheetRow(
    icon: ImageVector,
    label: String,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = iconTint)
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = contentColor,
        )
    }
}
