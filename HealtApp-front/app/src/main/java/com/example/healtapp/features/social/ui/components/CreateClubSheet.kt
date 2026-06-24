package com.example.healtapp.features.social.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.HowToVote
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.healtapp.core.ui.components.AppButton
import com.example.healtapp.core.ui.components.GradientFormPanel
import com.example.healtapp.core.ui.components.GradientOutlinedField
import com.example.healtapp.core.ui.components.PersonAvatar
import com.example.healtapp.core.ui.theme.MintPrimary
import com.example.healtapp.core.ui.theme.brandingGradient
import com.example.healtapp.core.ui.theme.contentPrimaryColor
import com.example.healtapp.core.ui.theme.contentSecondaryColor
import com.example.healtapp.core.ui.theme.subtleFillGradient

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreateClubSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (name: String, description: String?, rules: String?, avatarUri: Uri?) -> Unit,
) {
    if (!visible) return

    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var rules by remember { mutableStateOf("") }
    var avatarUri by remember { mutableStateOf<Uri?>(null) }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }

    val trimmedName = name.trim()
    val canCreate = trimmedName.length >= 3

    val pickImage = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri -> uri?.let { avatarUri = it } }

    val takePicture = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
    ) { success ->
        val uri = pendingCameraUri
        pendingCameraUri = null
        if (success && uri != null) avatarUri = uri
    }

    val requestCameraPermission = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            launchClubCamera(context) { uri ->
                pendingCameraUri = uri
                takePicture.launch(uri)
            }
        }
    }

    fun openGallery() {
        pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    fun openCamera() {
        when {
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED -> {
                launchClubCamera(context) { uri ->
                    pendingCameraUri = uri
                    takePicture.launch(uri)
                }
            }
            else -> requestCameraPermission.launch(Manifest.permission.CAMERA)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            CreateClubSheetHeader(onDismiss = onDismiss)
            CreateClubCommunityHero(avatarUri = avatarUri, clubName = trimmedName)
            CreateClubImagePicker(
                avatarUri = avatarUri,
                clubName = trimmedName,
                onGallery = ::openGallery,
                onCamera = ::openCamera,
                onClear = { avatarUri = null },
            )
            if (canCreate) {
                CreateClubPreviewCard(
                    name = trimmedName,
                    description = description.trim(),
                    avatarUri = avatarUri,
                )
            }
            GradientFormPanel {
                Text(
                    text = "1. Название клуба",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                GradientOutlinedField(
                    value = name,
                    onValueChange = { name = it },
                    label = "Как назовёте сообщество?",
                )

                Text(
                    text = "2. О чём клуб?",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                GradientOutlinedField(
                    value = description,
                    onValueChange = { description = it },
                    label = "Краткое описание для участников",
                    singleLine = false,
                    maxLines = 4,
                )

                Text(
                    text = "3. Правила",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "Необязательно — что важно соблюдать в клубе",
                    style = MaterialTheme.typography.bodySmall,
                    color = contentSecondaryColor(),
                )
                GradientOutlinedField(
                    value = rules,
                    onValueChange = { rules = it },
                    label = "Правила участия",
                    singleLine = false,
                    maxLines = 4,
                )
            }

            AppButton(
                text = when {
                    !canCreate -> "Введите название (от 3 символов)"
                    else -> "Создать клуб"
                },
                onClick = {
                    onConfirm(
                        trimmedName,
                        description.trim().takeIf { it.isNotBlank() },
                        rules.trim().takeIf { it.isNotBlank() },
                        avatarUri,
                    )
                },
                enabled = canCreate,
            )
        }
    }
}

@Composable
private fun CreateClubSheetHeader(onDismiss: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Brush.linearGradient(brandingGradient())),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.Groups,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(26.dp),
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Новый клуб",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Соберите людей вокруг общей цели или интереса",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        IconButton(onClick = onDismiss) {
            Icon(Icons.Filled.Close, contentDescription = "Закрыть")
        }
    }
}

@Composable
private fun CreateClubImagePicker(
    avatarUri: Uri?,
    clubName: String,
    onGallery: () -> Unit,
    onCamera: () -> Unit,
    onClear: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.96f))
            .border(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
                RoundedCornerShape(22.dp),
            )
            .padding(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            text = "Фото клуба",
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = "Выберите обложку из галереи или сделайте снимок",
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.bodySmall,
            color = contentSecondaryColor(),
        )

        Box(contentAlignment = Alignment.BottomEnd) {
            CreateClubAvatar(
                name = clubName.ifBlank { "Клуб" },
                avatarUri = avatarUri,
                size = 108.dp,
                cornerRadius = 28.dp,
            )
            if (avatarUri != null) {
                IconButton(
                    onClick = onClear,
                    modifier = Modifier
                        .padding(4.dp)
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)),
                ) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "Удалить фото",
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            FilledTonalButton(
                onClick = onGallery,
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Filled.PhotoLibrary, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Галерея")
            }
            FilledTonalButton(
                onClick = onCamera,
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Filled.PhotoCamera, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Камера")
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CreateClubCommunityHero(
    avatarUri: Uri?,
    clubName: String,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Brush.linearGradient(brandingGradient()))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CreateClubAvatar(
                name = clubName.ifBlank { "Клуб" },
                avatarUri = avatarUri,
                size = 72.dp,
                cornerRadius = 22.dp,
                placeholderIcon = Icons.Filled.Groups,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = "Сообщество по интересам",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
                Text(
                    text = if (avatarUri != null) {
                        "Ваша обложка уже на месте — клуб будет узнаваемым"
                    } else {
                        "Обсуждения, опросы и обмен достижениями — всё в одном месте"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.9f),
                )
            }
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            CreateClubFeatureChip(Icons.Filled.MenuBook, "Обсуждения")
            CreateClubFeatureChip(Icons.Filled.HowToVote, "Опросы")
            CreateClubFeatureChip(Icons.Filled.Groups, "Участники")
        }
    }
}

@Composable
private fun CreateClubFeatureChip(icon: ImageVector, label: String) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.18f))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = Color.White,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun CreateClubPreviewCard(
    name: String,
    description: String,
    avatarUri: Uri?,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(subtleFillGradient()))
            .border(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
                RoundedCornerShape(20.dp),
            )
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CreateClubAvatar(name = name, avatarUri = avatarUri, size = 52.dp)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = "Так будет выглядеть",
                style = MaterialTheme.typography.labelSmall,
                color = contentSecondaryColor(),
            )
            Text(
                text = name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = contentPrimaryColor(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = description.ifBlank { "Описание клуба появится здесь" },
                style = MaterialTheme.typography.bodySmall,
                color = contentSecondaryColor(),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "1 участник · вы основатель",
                style = MaterialTheme.typography.labelMedium,
                color = MintPrimary,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun CreateClubAvatar(
    name: String,
    avatarUri: Uri?,
    size: androidx.compose.ui.unit.Dp,
    cornerRadius: androidx.compose.ui.unit.Dp = size / 2,
    placeholderIcon: ImageVector = Icons.Filled.AddAPhoto,
) {
    val shape = RoundedCornerShape(cornerRadius)
    if (avatarUri != null) {
        AsyncImage(
            model = avatarUri,
            contentDescription = null,
            modifier = Modifier
                .size(size)
                .clip(shape)
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                    shape,
                ),
            contentScale = ContentScale.Crop,
        )
    } else if (name.isNotBlank() && name != "Клуб") {
        PersonAvatar(name = name, size = size, useGradient = true)
    } else {
        Box(
            modifier = Modifier
                .size(size)
                .clip(shape)
                .background(Color.White.copy(alpha = 0.2f))
                .border(
                    1.dp,
                    Color.White.copy(alpha = 0.25f),
                    shape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = placeholderIcon,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.9f),
                modifier = Modifier.size(size * 0.42f),
            )
        }
    }
}
