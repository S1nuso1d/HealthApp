package com.example.healtapp.features.onboarding.presentation

sealed interface OnboardingEvent {
    data class AgeChanged(val value: String) : OnboardingEvent
    data class SexChanged(val value: String) : OnboardingEvent
    data class HeightChanged(val value: String) : OnboardingEvent
    data class WeightChanged(val value: String) : OnboardingEvent
    data class TargetSleepChanged(val value: String) : OnboardingEvent
    data class TargetWaterChanged(val value: String) : OnboardingEvent
    data class GoalChanged(val value: String) : OnboardingEvent
    data class ActivityLevelChanged(val value: String) : OnboardingEvent
    data object Submit : OnboardingEvent
}