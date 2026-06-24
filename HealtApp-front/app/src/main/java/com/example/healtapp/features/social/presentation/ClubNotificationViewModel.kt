package com.example.healtapp.features.social.presentation

import androidx.lifecycle.ViewModel
import com.example.healtapp.features.social.data.ClubNotificationNotifier
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ClubNotificationViewModel @Inject constructor(
    private val notifier: ClubNotificationNotifier,
) : ViewModel() {
    val currentNotification = notifier.currentNotification

    fun dismissCurrent() = notifier.dismissCurrent()
}
