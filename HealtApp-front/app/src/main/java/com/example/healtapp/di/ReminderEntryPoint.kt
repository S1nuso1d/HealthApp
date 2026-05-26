package com.example.healtapp.di

import com.example.healtapp.data.preferences.NotificationPrefs
import com.example.healtapp.data.preferences.TokenStorage
import com.example.healtapp.domain.repository.ActivityRepository
import com.example.healtapp.domain.repository.HydrationRepository
import com.example.healtapp.domain.repository.MealRepository
import com.example.healtapp.domain.repository.ProfileRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface ReminderEntryPoint {
    fun tokenStorage(): TokenStorage
    fun mealRepository(): MealRepository
    fun hydrationRepository(): HydrationRepository
    fun activityRepository(): ActivityRepository
    fun profileRepository(): ProfileRepository
    fun notificationPrefs(): NotificationPrefs
}
