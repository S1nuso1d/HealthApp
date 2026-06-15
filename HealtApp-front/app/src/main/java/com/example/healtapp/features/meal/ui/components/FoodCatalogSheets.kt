package com.example.healtapp.features.meal.ui.components

import android.Manifest
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.healtapp.core.ui.components.AppButton
import com.example.healtapp.core.ui.components.AppCard
import com.example.healtapp.core.ui.components.AppTextField
import com.example.healtapp.core.ui.components.GradientFormPanel
import com.example.healtapp.core.ui.theme.brandingGradient
import com.example.healtapp.data.network.dto.meal.FoodCatalogItemDto
import com.example.healtapp.features.meal.presentation.MealUiState
import com.example.healtapp.features.meal.ui.BarcodeScannerSheet
import com.example.healtapp.features.meal.ui.getFileFromUri
import com.example.healtapp.features.meal.ui.hasCameraPermission
import java.io.File

@Composable
fun MealFoodCatalogHitRow(hit: FoodCatalogItemDto, onClick: () -> Unit) {
    AppCard(onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (!hit.imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = hit.imageUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(10.dp)),
                )
            } else {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .size(width = 4.dp, height = 40.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Brush.verticalGradient(brandingGradient())),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = hit.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = hit.macroSubtitle(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = "Выбрать",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodMacroCompletionSheet(
    visible: Boolean,
    uiState: MealUiState,
    onDismiss: () -> Unit,
    onProteinChange: (String) -> Unit,
    onFatChange: (String) -> Unit,
    onCarbsChange: (String) -> Unit,
    onSave: () -> Unit,
) {
    if (!visible) return
    val item = uiState.selectedCatalogItem ?: return
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            MealSheetHeader(
                title = "Дополнить БЖУ",
                subtitle = item.name,
                icon = Icons.Filled.Edit,
                onDismiss = onDismiss,
            )
            AppCard {
                Text(
                    text = "Open Food Facts дал только калории. Укажите белки, жиры и углеводы на 100 г — при следующем поиске они подтянутся из каталога.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            uiState.calories.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = "Калории: $it ккал / 100 г",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            GradientFormPanel {
                AppTextField(uiState.macroCompletionProtein, onProteinChange, label = "Белки, г / 100 г")
                AppTextField(uiState.macroCompletionFat, onFatChange, label = "Жиры, г / 100 г")
                AppTextField(uiState.macroCompletionCarbs, onCarbsChange, label = "Углеводы, г / 100 г")
            }
            AppButton(
                text = if (uiState.isCatalogSaving) "Сохраняем…" else "Сохранить в каталог",
                onClick = onSave,
                enabled = !uiState.isCatalogSaving,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCustomFoodSheet(
    visible: Boolean,
    uiState: MealUiState,
    onDismiss: () -> Unit,
    onSave: (
        name: String,
        barcode: String?,
        brand: String?,
        calories100g: Float?,
        proteinG100g: Float?,
        fatG100g: Float?,
        carbsG100g: Float?,
        photoFile: File?,
    ) -> Unit,
) {
    if (!visible) return
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    var name by remember(visible) { mutableStateOf("") }
    var barcode by remember(visible) { mutableStateOf("") }
    var brand by remember(visible) { mutableStateOf("") }
    var calories by remember(visible) { mutableStateOf("") }
    var protein by remember(visible) { mutableStateOf("") }
    var fat by remember(visible) { mutableStateOf("") }
    var carbs by remember(visible) { mutableStateOf("") }
    var photoFile by remember(visible) { mutableStateOf<File?>(null) }
    var photoPreview by remember(visible) { mutableStateOf<Uri?>(null) }
    var showBarcodeScanner by remember(visible) { mutableStateOf(false) }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) showBarcodeScanner = true
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        photoPreview = uri
        photoFile = uri?.let { getFileFromUri(context, it) }
    }

    fun requestBarcodeScan() {
        if (hasCameraPermission(context)) {
            showBarcodeScanner = true
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            MealSheetHeader(
                title = "Свой продукт",
                subtitle = "Сохранится в каталоге — можно найти по штрихкоду",
                icon = Icons.Filled.Restaurant,
                onDismiss = onDismiss,
            )

            AppCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (photoPreview != null) {
                        AsyncImage(
                            model = photoPreview,
                            contentDescription = null,
                            modifier = Modifier
                                .size(72.dp)
                                .clip(RoundedCornerShape(14.dp)),
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .clickable {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                                    )
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Filled.AddPhotoAlternate, contentDescription = "Фото")
                        }
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        AppTextField(name, { name = it }, label = "Название")
                        AppTextField(brand, { brand = it }, label = "Бренд (необязательно)")
                    }
                }
            }

            AppCard {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Штрихкод",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "Отсканируйте или введите вручную — продукт будет находиться при следующем сканировании.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AppTextField(
                            value = barcode,
                            onValueChange = { barcode = it },
                            label = "Код EAN / UPC",
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(onClick = ::requestBarcodeScan) {
                            Icon(
                                Icons.Filled.QrCodeScanner,
                                contentDescription = "Сканировать штрихкод",
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                    AppButton(
                        text = "Сканировать штрихкод",
                        onClick = ::requestBarcodeScan,
                        isSecondary = true,
                    )
                }
            }

            GradientFormPanel {
                AppTextField(calories, { calories = it }, label = "Калории / 100 г")
                AppTextField(protein, { protein = it }, label = "Белки / 100 г")
                AppTextField(fat, { fat = it }, label = "Жиры / 100 г")
                AppTextField(carbs, { carbs = it }, label = "Углеводы / 100 г")
            }

            AppButton(
                text = if (uiState.isCatalogSaving) "Сохраняем…" else "Добавить в каталог",
                onClick = {
                    onSave(
                        name,
                        barcode,
                        brand,
                        calories.toFloatOrNull(),
                        protein.toFloatOrNull(),
                        fat.toFloatOrNull(),
                        carbs.toFloatOrNull(),
                        photoFile,
                    )
                },
                enabled = !uiState.isCatalogSaving && name.isNotBlank(),
            )
        }
    }

    BarcodeScannerSheet(
        visible = showBarcodeScanner,
        onDismiss = { showBarcodeScanner = false },
        onBarcode = { code ->
            barcode = code
            showBarcodeScanner = false
        },
    )
}
