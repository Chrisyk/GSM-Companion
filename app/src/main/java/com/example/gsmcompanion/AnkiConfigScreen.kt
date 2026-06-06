package com.example.gsmcompanion

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavController

@Composable
fun AnkiConfigScreen(
    navController : NavController,
    viewModel : AnkiConfigViewModel
) {
    LaunchedEffect(Unit) {
        val ankiModels = viewModel.getModelNames()
    }

}