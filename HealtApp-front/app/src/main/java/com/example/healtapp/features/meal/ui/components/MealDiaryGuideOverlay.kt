package com.example.healtapp.features.meal.ui.components

import android.content.Context
import androidx.compose.runtime.Composable
import com.example.healtapp.core.ui.components.FeatureGuideContent
import com.example.healtapp.core.ui.components.FeatureGuideOverlay
import com.example.healtapp.core.ui.components.FeatureGuidePrefs
import com.example.healtapp.core.ui.components.FeatureGuideScreen

/** @deprecated Используйте [FeatureGuidePrefs] и [FeatureGuideOverlay]. */
object MealDiaryGuidePrefs {
    fun hasSeen(context: Context): Boolean =
        FeatureGuidePrefs.hasSeen(context, FeatureGuideScreen.Nutrition)

    fun shouldShow(context: Context): Boolean =
        FeatureGuidePrefs.shouldShow(context, FeatureGuideScreen.Nutrition)

    fun markSeen(context: Context) {
        FeatureGuidePrefs.markSeen(context, FeatureGuideScreen.Nutrition)
    }

    fun requestShowAgain(context: Context) {
        FeatureGuidePrefs.requestShowAgain(context, FeatureGuideScreen.Nutrition)
    }
}

@Composable
fun MealDiaryGuideOverlay(
    visible: Boolean,
    onDismiss: () -> Unit,
) {
    FeatureGuideOverlay(
        visible = visible,
        pages = FeatureGuideContent.nutrition,
        sectionLabel = "Питание",
        onDismiss = onDismiss,
    )
}
