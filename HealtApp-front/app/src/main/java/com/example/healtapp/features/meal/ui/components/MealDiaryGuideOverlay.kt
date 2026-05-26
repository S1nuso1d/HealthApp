package com.example.healtapp.features.meal.ui.components

import android.content.Context
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.healtapp.core.ui.components.AppButton
import com.example.healtapp.core.ui.theme.MintPrimary
import com.example.healtapp.core.ui.theme.SkyPrimary
import com.example.healtapp.core.ui.theme.brandingGradient
import com.example.healtapp.core.ui.theme.screenBackgroundGradient

object MealDiaryGuidePrefs {
    const val PREFS_NAME = "nutrition_guide_prefs"
    const val SEEN_KEY = "meal_diary_intro_seen"
    private const val REQUEST_SHOW_KEY = "meal_diary_intro_request_show"

    fun hasSeen(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(SEEN_KEY, false)

    fun shouldShow(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(REQUEST_SHOW_KEY, false) || !hasSeen(context)

    fun markSeen(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(SEEN_KEY, true)
            .putBoolean(REQUEST_SHOW_KEY, false)
            .apply()
    }

    /** Показать гайд при следующем открытии раздела «Питание». */
    fun requestShowAgain(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(REQUEST_SHOW_KEY, true)
            .apply()
    }
}

private data class MealGuidePage(
    val title: String,
    val lead: String,
    val bullets: List<String>,
    val icon: ImageVector,
    val miniIcons: List<ImageVector> = emptyList(),
)

private val guidePages = listOf(
    MealGuidePage(
        title = "Дневник питания",
        lead = "Записи за день помогают видеть реальные калории и БЖУ, а не «примерно». Чем чаще вы вносите еду, тем точнее цели и рекомендации.",
        bullets = listOf(
            "Фиксируйте всё, что съели и выпили в течение дня.",
            "Не ждите идеальной точности — лучше примерная запись, чем пропуск.",
        ),
        icon = Icons.Filled.Restaurant,
    ),
    MealGuidePage(
        title = "Что указывать в записи",
        lead = "Для каждого продукта или блюда заполните понятное название и вес порции — от этого считаются калории и БЖУ.",
        bullets = listOf(
            "Название: «Овсянка на молоке», «Куриная грудка», «Яблоко».",
            "Вес порции в граммах — взвесьте или оцените по упаковке.",
            "При необходимости уточните белки, жиры, углеводы и кофеин.",
            "Время приёма подставится автоматически, его можно изменить.",
        ),
        icon = Icons.Filled.Scale,
    ),
    MealGuidePage(
        title = "Масло, соусы и жир для готовки",
        lead = "Всё, что добавляете при готовке и что попадает в тарелку, нужно учитывать. Масло на сковороде часто забывают — без него калории занижаются.",
        bullets = listOf(
            "Масло для жарки, сливочное масло, маргарин — отдельной строкой с весом в граммах.",
            "Соусы (майонез, сметана, кетчуп) — по фактически съеденному количеству.",
            "Если часть масла осталась в сковороде, укажите только то, что съели (например, 5–10 г вместо 20 г).",
        ),
        icon = Icons.Filled.LocalFireDepartment,
    ),
    MealGuidePage(
        title = "Сложные и домашние блюда",
        lead = "Суп, салат, запеканка из нескольких продуктов удобнее собрать один раз во вкладке «Мои блюда».",
        bullets = listOf(
            "Добавьте каждый ингредиент с весом — приложение посчитает КБЖУ на порцию.",
            "Сохраните блюдо и в дневнике выбирайте готовую порцию в один тап.",
            "Вес порции можно подправить, если съели не всю кастрюлю.",
        ),
        icon = Icons.Filled.MenuBook,
    ),
    MealGuidePage(
        title = "Как быстро найти продукт",
        lead = "Ищите по названию, штрихкоду или сохранённым блюдам — не обязательно вводить КБЖУ вручную.",
        bullets = listOf(
            "Поиск по названию — для продуктов из базы и недавних записей.",
            "Сканер штрихкода — для упакованных товаров с этикеткой.",
            "«Мои блюда» — для домашних рецептов и повторяющихся обедов.",
        ),
        icon = Icons.Filled.Search,
        miniIcons = listOf(Icons.Filled.Search, Icons.Filled.QrCodeScanner, Icons.Filled.MenuBook),
    ),
)

@Composable
fun MealDiaryGuideOverlay(
    visible: Boolean,
    onDismiss: () -> Unit,
) {
    if (!visible) return

    var pageIndex by remember { mutableIntStateOf(0) }
    val isFirst = pageIndex == 0
    val isLast = pageIndex == guidePages.lastIndex

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.58f)),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 28.dp),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
            tonalElevation = 6.dp,
            shadowElevation = 12.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(screenBackgroundGradient()))
                    .padding(horizontal = 20.dp, vertical = 16.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Шаг ${pageIndex + 1} из ${guidePages.size}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                    )
                    TextButton(onClick = onDismiss) {
                        Text("Пропустить")
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                ) {
                    AnimatedContent(
                        targetState = pageIndex,
                        modifier = Modifier.fillMaxSize(),
                        transitionSpec = {
                            val forward = targetState > initialState
                            val enterOffset: (Int) -> Int = { full -> if (forward) full / 3 else -full / 3 }
                            val exitOffset: (Int) -> Int = { full -> if (forward) -full / 3 else full / 3 }
                            (slideInHorizontally(initialOffsetX = enterOffset) + fadeIn())
                                .togetherWith(slideOutHorizontally(targetOffsetX = exitOffset) + fadeOut())
                        },
                        label = "mealGuidePage",
                    ) { index ->
                        val current = guidePages[index]
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            GuideHeroIcon(
                                icon = current.icon,
                                miniIcons = current.miniIcons,
                            )
                            Text(
                                text = current.title,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = current.lead,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            current.bullets.forEach { bullet ->
                                GuideBulletRow(text = bullet)
                            }
                        }
                    }
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    GuidePageDots(
                        count = guidePages.size,
                        selected = pageIndex,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        IconButton(
                            onClick = { if (!isFirst) pageIndex -= 1 },
                            enabled = !isFirst,
                            modifier = Modifier.size(48.dp),
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Назад",
                                tint = if (isFirst) {
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.28f)
                                } else {
                                    MaterialTheme.colorScheme.primary
                                },
                            )
                        }

                        AppButton(
                            text = "Далее",
                            onClick = {
                                if (isLast) onDismiss() else pageIndex += 1
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(54.dp),
                        )

                        IconButton(
                            onClick = { if (!isLast) pageIndex += 1 },
                            enabled = !isLast,
                            modifier = Modifier.size(48.dp),
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Вперёд",
                                tint = if (isLast) {
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.28f)
                                } else {
                                    MaterialTheme.colorScheme.primary
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GuideHeroIcon(
    icon: ImageVector,
    miniIcons: List<ImageVector>,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (miniIcons.size > 1) 120.dp else 108.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(RoundedCornerShape(26.dp))
                .background(Brush.linearGradient(brandingGradient()))
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(listOf(MintPrimary.copy(0.4f), SkyPrimary.copy(0.4f))),
                    shape = RoundedCornerShape(26.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = Color.White,
            )
        }
        if (miniIcons.size > 1) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                miniIcons.forEach { mini ->
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f))
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                                RoundedCornerShape(12.dp),
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = mini,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GuideBulletRow(text: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box(
            modifier = Modifier
                .padding(top = 6.dp)
                .size(8.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(listOf(MintPrimary, SkyPrimary)),
                ),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun GuidePageDots(
    count: Int,
    selected: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(count) { index ->
            val active = index == selected
            Box(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .size(if (active) 10.dp else 7.dp)
                    .clip(CircleShape)
                    .background(
                        if (active) {
                            Brush.linearGradient(brandingGradient())
                        } else {
                            Brush.linearGradient(
                                listOf(
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                                ),
                            )
                        },
                    ),
            )
        }
    }
}
