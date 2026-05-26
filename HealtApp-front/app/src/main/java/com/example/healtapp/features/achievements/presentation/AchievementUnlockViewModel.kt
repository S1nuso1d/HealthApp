package com.example.healtapp.features.achievements.presentation

import androidx.lifecycle.ViewModel
import com.example.healtapp.features.achievements.data.AchievementUnlockNotifier
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AchievementUnlockViewModel @Inject constructor(
    private val notifier: AchievementUnlockNotifier,
) : ViewModel() {
    val currentUnlock = notifier.currentUnlock

    fun dismissCurrent() = notifier.dismissCurrent()
}
