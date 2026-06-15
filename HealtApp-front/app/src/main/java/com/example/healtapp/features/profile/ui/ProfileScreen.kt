package com.example.healtapp.features.profile.ui

import android.content.pm.PackageManager
import android.Manifest
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.HowToVote
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.health.connect.client.PermissionController
import com.example.healtapp.BuildConfig
import com.example.healtapp.core.common.Constants
import com.example.healtapp.core.ui.components.AppButton
import com.example.healtapp.core.ui.components.AppCard
import com.example.healtapp.core.ui.components.AppMessageBanner
import com.example.healtapp.core.ui.components.AppMessageType
import com.example.healtapp.core.ui.components.AppScreen
import com.example.healtapp.core.ui.components.FeatureGuideContent
import com.example.healtapp.core.ui.components.FeatureGuideOverlay
import com.example.healtapp.core.ui.components.FeatureGuidePrefs
import com.example.healtapp.core.ui.components.FeatureGuideScreen
import com.example.healtapp.core.ui.components.SectionHeader
import com.example.healtapp.di.ApiServerConfigEntryPoint
import com.example.healtapp.di.ImageLoaderEntryPoint
import com.example.healtapp.features.auth.ui.components.ChangePasswordForm
import com.example.healtapp.features.auth.ui.components.ChangePasswordFormHeader
import com.example.healtapp.features.profile.PoliticalRecommendations
import com.example.healtapp.features.profile.presentation.ProfileEditViewModel
import com.example.healtapp.features.profile.ui.components.ProfileBodyStatsCard
import com.example.healtapp.features.profile.ui.components.ProfileDietSection
import com.example.healtapp.features.profile.ui.components.ProfileGoalsEditSheet
import com.example.healtapp.features.profile.ui.components.ProfileGoalsHabitsSection
import com.example.healtapp.features.profile.ui.components.ProfileGoalsStrip
import com.example.healtapp.features.profile.ui.components.ProfileHeroBlock
import com.example.healtapp.features.profile.ui.components.ProfileLogoutCard
import com.example.healtapp.features.profile.ui.components.ProfileNavLink
import com.example.healtapp.features.profile.ui.components.ProfilePersonalDataSection
import com.example.healtapp.data.preferences.DashboardUiPrefs
import com.example.healtapp.features.profile.ui.components.ProfileDashboardLayoutSelector
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
    onOpenClubs: () -> Unit = {},
    onOpenPoliticalRecommendations: () -> Unit = {},
    onOpenPills: () -> Unit = {},
    onOpenCycle: () -> Unit = {},
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

    val displayName = uiState.publicDisplayName.ifBlank {
        listOf(uiState.firstName, uiState.lastName)
            .filter { it.isNotBlank() }
            .joinToString(" ")
            .ifBlank { uiState.nickname }
    }

    val initial = when {
        displayName.isNotBlank() -> displayName.first().uppercaseChar().toString()
        uiState.goal.isNotBlank() -> uiState.goal.first().uppercaseChar().toString()
        uiState.age.isNotBlank() -> uiState.age.first().toString()
        else -> "Я"
    }

    var showAvatarSheet by remember { mutableStateOf(false) }
    var dashboardLayout by remember { mutableStateOf(DashboardUiPrefs.getLayoutMode(context)) }
    var showGoalsSheet by remember { mutableStateOf(false) }
    var goalsCardExpanded by remember { mutableStateOf(false) }
    var basicsCardExpanded by remember { mutableStateOf(false) }
    var dietCardExpanded by remember { mutableStateOf(false) }
    var goalsSavePending by remember { mutableStateOf(false) }
    var collapsingSection by remember { mutableStateOf<ProfileSaveSection?>(null) }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
    var showGuide by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        showGuide = FeatureGuidePrefs.shouldShow(context, FeatureGuideScreen.Profile)
    }

    LaunchedEffect(uiState.isSaving, uiState.success) {
        if (goalsSavePending && !uiState.isSaving && uiState.success != null) {
            showGoalsSheet = false
            goalsSavePending = false
        }
        if (!uiState.isSaving && uiState.success != null) {
            when (collapsingSection) {
                ProfileSaveSection.BASICS -> basicsCardExpanded = false
                ProfileSaveSection.GOALS -> goalsCardExpanded = false
                ProfileSaveSection.DIET -> dietCardExpanded = false
                null -> {}
            }
            collapsingSection = null
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

    val requestHealthPermissions = rememberLauncherForActivityResult(
        PermissionController.createRequestPermissionResultContract(),
    ) { granted ->
        if (granted.containsAll(viewModel.healthConnectPermissions)) {
            viewModel.syncHealthConnect()
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

    Box(modifier = Modifier.fillMaxSize()) {
        AppScreen(
            title = "Профиль",
            subtitle = "Данные · цели · настройки",
            scrollable = true,
            scrollStateKey = "profile",
        ) {
            ProfileHeroBlock(
                initial = initial,
                displayName = displayName,
                avatarUrl = avatarUrl,
                imageLoader = imageLoader,
                goal = uiState.goal,
                activityLevel = uiState.activityLevel,
                age = uiState.age,
                guestMode = uiState.guestMode,
                isUploadingAvatar = uiState.isUploadingAvatar,
                enabled = avatarEnabled,
                onAvatarClick = { if (avatarEnabled) showAvatarSheet = true },
            )

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

            SectionHeader(
                title = "Личные данные",
                subtitle = "Имя, возраст и параметры тела",
            )

            ProfilePersonalDataSection(
                uiState = uiState,
                expanded = basicsCardExpanded,
                onExpandedChange = { basicsCardExpanded = it },
                onFirstNameChange = viewModel::updateFirstName,
                onLastNameChange = viewModel::updateLastName,
                onNicknameChange = viewModel::updateNickname,
                onBirthDateChange = viewModel::updateBirthDate,
                onHeightChange = viewModel::updateHeight,
                onWeightChange = viewModel::updateWeight,
                onSexChange = viewModel::updateSex,
                onSave = {
                    collapsingSection = ProfileSaveSection.BASICS
                    viewModel.save()
                },
            )

            ProfileGoalsHabitsSection(
                uiState = uiState,
                expanded = goalsCardExpanded,
                onExpandedChange = { goalsCardExpanded = it },
                onGoalChange = viewModel::updateGoal,
                onActivityLevelChange = viewModel::updateActivityLevel,
                onSave = {
                    collapsingSection = ProfileSaveSection.GOALS
                    viewModel.save()
                },
            )

            ProfileDietSection(
                uiState = uiState,
                expanded = dietCardExpanded,
                onExpandedChange = { dietCardExpanded = it },
                onIsVegetarianChange = viewModel::updateIsVegetarian,
                onHasAllergiesChange = viewModel::updateHasAllergies,
                onAllergiesTextChange = viewModel::updateAllergiesText,
                onSave = {
                    collapsingSection = ProfileSaveSection.DIET
                    viewModel.save()
                },
            )

            SectionHeader(
                title = "Аккаунт",
                subtitle = "Оформление и безопасность",
            )

            ProfileExpandableCard(
                title = "Оформление",
                icon = Icons.Filled.Palette,
                initiallyExpanded = false,
            ) {
                ProfileThemeSelector(
                    selected = uiState.themeMode,
                    onSelected = viewModel::setThemeMode,
                )
                Spacer(modifier = Modifier.height(16.dp))
                ProfileDashboardLayoutSelector(
                    selected = dashboardLayout,
                    onSelected = { mode ->
                        DashboardUiPrefs.setLayoutMode(context, mode)
                        dashboardLayout = mode
                    },
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
                title = "Здоровье",
                subtitle = "Напоминания, цикл и добавки",
            )

            AppCard {
                Column {
                    ProfileNavLink(
                        icon = Icons.Filled.Medication,
                        title = "Витамины и таблетки",
                        subtitle = "Напоминания о приёме",
                        onClick = onOpenPills,
                        showDivider = false,
                    )
                    if (uiState.sex == Constants.Sex.FEMALE) {
                        ProfileCardDivider()
                        ProfileNavLink(
                            icon = Icons.Filled.CalendarMonth,
                            title = "Женское здоровье",
                            subtitle = "Менструальный календарь и цикл",
                            onClick = onOpenCycle,
                            showDivider = false,
                        )
                    }
                }
            }

            SectionHeader(
                title = "Сообщество",
                subtitle = "Достижения, друзья и клубы",
            )

            AppCard {
                Column {
                    ProfileNavLink(
                        icon = Icons.Filled.EmojiEvents,
                        title = "Достижения",
                        subtitle = "Серии, цели и рекорды",
                        onClick = onOpenAchievements,
                        showDivider = false,
                    )
                    ProfileCardDivider()
                    ProfileNavLink(
                        icon = Icons.Filled.Group,
                        title = "Друзья",
                        subtitle = "Поиск, заявки и челлендж",
                        onClick = onOpenFriends,
                        showDivider = false,
                    )
                    ProfileCardDivider()
                    ProfileNavLink(
                        icon = Icons.Filled.Groups,
                        title = "Клубы",
                        subtitle = "Обсуждения, опросы и достижения",
                        onClick = onOpenClubs,
                        showDivider = false,
                    )
                }
            }

            SectionHeader(
                title = "Сервис",
                subtitle = "Синхронизация, интеграции и обучение",
            )

            AppCard {
                Column {
                    AppButton(
                        text = if (uiState.isLoading) "Синхронизация..." else "Синхронизировать с Health Connect",
                        onClick = {
                            viewModel.onSyncHealthConnectClicked { permissions ->
                                requestHealthPermissions.launch(permissions)
                            }
                        },
                        enabled = !uiState.isLoading && !uiState.guestMode,
                        modifier = Modifier.padding(16.dp),
                    )
                    ProfileCardDivider()
                    ProfileNavLink(
                        icon = Icons.Filled.Description,
                        title = "Конфиденциальность",
                        subtitle = "Данные, аккаунт и удаление",
                        onClick = onOpenDataPrivacy,
                        showDivider = false,
                    )
                    ProfileCardDivider()
                    ProfileNavLink(
                        icon = Icons.Filled.Link,
                        title = "Интеграции",
                        subtitle = "Health Connect, FatSecret, Mi Band BLE",
                        onClick = onOpenIntegrations,
                        showDivider = false,
                    )
                    ProfileCardDivider()
                    ProfileNavLink(
                        icon = Icons.Filled.Watch,
                        title = "Умные часы и трекеры",
                        subtitle = "Mi Band, Garmin, Health Connect",
                        onClick = onOpenMiBandBle,
                        showDivider = false,
                    )
                    ProfileCardDivider()
                    ProfileNavLink(
                        icon = Icons.Filled.Notifications,
                        title = "Уведомления",
                        subtitle = "Вода, еда, советы",
                        onClick = onOpenNotifications,
                        showDivider = false,
                    )
                    ProfileCardDivider()
                    ProfileNavLink(
                        icon = Icons.Filled.MenuBook,
                        title = "Обучение по разделам",
                        subtitle = "Показать подсказки снова на всех экранах",
                        onClick = {
                            FeatureGuidePrefs.requestShowAllAgain(context)
                            showGuide = true
                        },
                        showDivider = false,
                    )
                    ProfileCardDivider()
                    ProfileNavLink(
                        icon = Icons.Filled.HowToVote,
                        title = "Политические рекомендации",
                        subtitle = PoliticalRecommendations.previewSubtitle(
                            activityLevel = uiState.activityLevel,
                            goal = uiState.goal.ifBlank { Constants.Goals.IMPROVE_ENERGY },
                        ),
                        onClick = onOpenPoliticalRecommendations,
                        showDivider = false,
                    )
                }
            }

            ProfileLogoutCard(
                enabled = !uiState.isLoading && !uiState.isSaving &&
                    !uiState.isUploadingAvatar && !uiState.isChangingPassword,
                onLogout = { viewModel.logout(onLogout) },
            )
        }

        FeatureGuideOverlay(
            visible = showGuide,
            pages = FeatureGuideContent.profile,
            sectionLabel = "Профиль",
            onDismiss = {
                FeatureGuidePrefs.markSeen(context, FeatureGuideScreen.Profile)
                showGuide = false
            },
        )
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
private fun ProfileCardDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(vertical = 4.dp),
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
    )
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

private enum class ProfileSaveSection {
    BASICS,
    GOALS,
    DIET,
}
