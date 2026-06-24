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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.example.healtapp.BuildConfig
import com.example.healtapp.core.ui.components.PersonAvatar
import com.example.healtapp.core.ui.theme.contentSecondaryColor

fun createSocialCameraUri(context: android.content.Context, filePrefix: String): Uri {
    val dir = java.io.File(context.cacheDir, "camera").apply { mkdirs() }
    val file = java.io.File(dir, "${filePrefix}_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(
        context,
        "${BuildConfig.APPLICATION_ID}.fileprovider",
        file,
    )
}

fun launchClubCamera(context: android.content.Context, onUri: (Uri) -> Unit) {
    onUri(createSocialCameraUri(context, "club_avatar"))
}

@Composable
fun rememberSocialCameraLauncher(
    filePrefix: String,
    onCaptured: (Uri) -> Unit,
): () -> Unit {
    val context = LocalContext.current
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
    ) { success ->
        val uri = pendingCameraUri
        pendingCameraUri = null
        if (success && uri != null) onCaptured(uri)
    }

    val requestCameraPermission = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            val uri = createSocialCameraUri(context, filePrefix)
            pendingCameraUri = uri
            cameraLauncher.launch(uri)
        }
    }

    return {
        when {
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED -> {
                val uri = createSocialCameraUri(context, filePrefix)
                pendingCameraUri = uri
                cameraLauncher.launch(uri)
            }
            else -> requestCameraPermission.launch(Manifest.permission.CAMERA)
        }
    }
}

@Composable
fun ClubImagePickerSection(
    clubName: String,
    avatarUri: Uri?,
    existingAvatarUrl: String? = null,
    onAvatarChange: (Uri?) -> Unit,
    modifier: Modifier = Modifier,
    previewSize: Dp = 108.dp,
) {
    val context = LocalContext.current
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }

    val pickImage = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri -> uri?.let { onAvatarChange(it) } }

    val takePicture = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
    ) { success ->
        val uri = pendingCameraUri
        pendingCameraUri = null
        if (success && uri != null) onAvatarChange(uri)
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

    Column(
        modifier = modifier
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
            text = "Нажмите на обложку или выберите из галереи / камеры",
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.bodySmall,
            color = contentSecondaryColor(),
        )

        Box(contentAlignment = Alignment.BottomEnd) {
            ClubPickerAvatar(
                name = clubName.ifBlank { "Клуб" },
                avatarUri = avatarUri,
                existingAvatarUrl = existingAvatarUrl,
                size = previewSize,
                cornerRadius = 28.dp,
            )
            if (avatarUri != null) {
                IconButton(
                    onClick = { onAvatarChange(null) },
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
                onClick = ::openGallery,
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Filled.PhotoLibrary, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Галерея")
            }
            FilledTonalButton(
                onClick = ::openCamera,
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Filled.PhotoCamera, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Камера")
            }
        }
    }
}

@Composable
private fun ClubPickerAvatar(
    name: String,
    avatarUri: Uri?,
    existingAvatarUrl: String?,
    size: Dp,
    cornerRadius: Dp,
    placeholderIcon: ImageVector = Icons.Filled.AddAPhoto,
) {
    val shape = RoundedCornerShape(cornerRadius)
    val clickableModifier = Modifier
        .size(size)
        .clip(shape)
        .border(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
            shape,
        )

    when {
        avatarUri != null -> {
            AsyncImage(
                model = avatarUri,
                contentDescription = null,
                modifier = clickableModifier,
                contentScale = ContentScale.Crop,
            )
        }
        !existingAvatarUrl.isNullOrBlank() -> {
            AsyncImage(
                model = existingAvatarUrl,
                contentDescription = null,
                modifier = clickableModifier,
                contentScale = ContentScale.Crop,
            )
        }
        name.isNotBlank() && name != "Клуб" -> {
            PersonAvatar(
                name = name,
                size = size,
                useGradient = true,
                modifier = Modifier
                    .clip(shape)
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                        shape,
                    ),
            )
        }
        else -> {
            Box(
                modifier = clickableModifier
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
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(size * 0.42f),
                )
            }
        }
    }
}
