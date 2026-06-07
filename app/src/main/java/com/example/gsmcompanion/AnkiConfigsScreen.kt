package com.example.gsmcompanion

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Person
import androidx.compose.ui.Alignment

@Composable
fun AnkiConfigsScreen(
    navController : NavController,
    viewModel : AnkiConfigsViewModel = viewModel()
) {

    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.getModelNames()
    }
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            BottomBar(navController)
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier
                .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            )
            {
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterStart
                ) {
                    uiState.selectedModel?.let { selectedModel ->
                        Text("Selected model: $selectedModel")
                    }
                }

                if (uiState.isLoadingModels) {
                    Text("Loading models...")
                } else {
                    ModelSelectorMenu(
                        modelNames = uiState.modelNames,
                        onModelSelected = viewModel::selectModel
                    )
                }
            }


            if (uiState.isLoadingFields) {
                Text("Loading fields...")
            } else {
                uiState.fieldNames.forEach { fieldName ->
                    Text(fieldName)
                }
            }
            uiState.errorMessage?.let { error ->
                Text(error)
            }
        }
    }
}

@Composable
fun ModelSelectorMenu(
    modelNames: List<String>,
    onModelSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .padding(16.dp)
    ) {
        IconButton(onClick = { expanded = !expanded }) {
            Icon(Icons.Default.MoreVert, contentDescription = "More options")
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {

            modelNames.forEach { modelName ->
                DropdownMenuItem(
                    text = { Text(modelName) },
                    leadingIcon = { Icon(Icons.Outlined.Person, contentDescription = null) },
                    onClick = {
                        onModelSelected(modelName)
                        expanded = false
                    }
                )
            }
        }
    }
}