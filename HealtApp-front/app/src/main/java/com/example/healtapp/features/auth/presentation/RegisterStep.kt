package com.example.healtapp.features.auth.presentation

enum class RegisterStep(val index: Int, val title: String) {
    Credentials(0, "Аккаунт"),
    Verify(1, "Подтверждение"),
    Profile(2, "О вас"),
    Dietary(3, "Питание"),
    HealthConnect(4, "Health Connect"),
    ;

    companion object {
        val wizardSteps = entries.filter { it != Verify }
        const val totalWizardSteps = 4
    }
}
