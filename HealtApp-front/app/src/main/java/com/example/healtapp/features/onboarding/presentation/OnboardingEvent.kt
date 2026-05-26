package com.example.healtapp.features.onboarding.presentation

sealed interface OnboardingEvent {
    data object NextStep : OnboardingEvent
    data object PrevStep : OnboardingEvent
    data class VegetarianChanged(val value: Boolean) : OnboardingEvent
    data class HasAllergiesChanged(val value: Boolean) : OnboardingEvent
    data class AllergiesTextChanged(val value: String) : OnboardingEvent
    data class AgeChanged(val value: String) : OnboardingEvent
    data class SexChanged(val value: String) : OnboardingEvent
    data class HeightChanged(val value: String) : OnboardingEvent
    data class WeightChanged(val value: String) : OnboardingEvent
    data class GoalChanged(val value: String) : OnboardingEvent
    data class ActivityLevelChanged(val value: String) : OnboardingEvent
    data object Submit : OnboardingEvent
}
