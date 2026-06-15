package com.example.healtapp.features.fasting.ui



import androidx.compose.material.icons.Icons

import androidx.compose.material.icons.filled.Timer

import androidx.compose.runtime.Composable

import androidx.hilt.navigation.compose.hiltViewModel

import com.example.healtapp.core.ui.components.AppScreen

import com.example.healtapp.features.fasting.presentation.FastingViewModel



@Composable

fun FastingScreen(

    onBack: () -> Unit,

    viewModel: FastingViewModel = hiltViewModel(),

) {

    AppScreen(

        title = "Интервальное голодание",

        subtitle = "Таймер окна питания",

        onNavigateBack = onBack,

        headerIcon = Icons.Filled.Timer,

    ) {

        FastingTabContent(viewModel = viewModel)

    }

}

