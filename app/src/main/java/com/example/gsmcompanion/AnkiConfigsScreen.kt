package com.example.gsmcompanion

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import kotlinx.coroutines.launch

@Composable
fun AnkiConfigsScreen(
    navController: NavController,
    viewModel: AnkiConfigsViewModel = viewModel()
) {

    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        viewModel.getModelDeckNames()
    }
    LaunchedEffect(uiState.dataStoreMessage) {
        if (uiState.dataStoreMessage != null) {
            Toast.makeText(context, uiState.dataStoreMessage, Toast.LENGTH_SHORT).show()
            viewModel.consumeDataStoreMessage()
        }
    }

    LaunchedEffect(uiState.selectedModel) {
        val selectedModel = uiState.selectedModel
        if (selectedModel != null) {
            viewModel.getFieldNames(selectedModel)
            viewModel.loadFields(selectedModel)
        }
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
                    val selectedModel = uiState.selectedModel
                    Text("Selected model: ${selectedModel ?: "None"}")

                }

                Button(
                    onClick = { scope.launch { viewModel.getFieldValuesYomitan() } },
                ) { Text("Yomitan Fetch") }

                IconButton(onClick = { scope.launch { viewModel.saveFields() } }) {
                    Icon(Icons.Default.Done, contentDescription = "Save")
                }

                if (uiState.isLoadingModelsDecks) {
                    Text(
                        modifier = Modifier
                            .padding(16.dp),
                        text = "Loading models..."
                    )
                } else {
                    ModelSelectorMenu(
                        modelNames = uiState.modelNames,
                        onModelSelected = viewModel::selectModel
                    )
                }

            }

            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterStart
                ) {
                    val selectedDeck = uiState.selectedDeck
                    Text("Selected deck: ${selectedDeck ?: "None"}")
                }

                if (uiState.isLoadingModelsDecks) {
                    Text(
                        modifier = Modifier
                            .padding(16.dp),
                        text = "Loading decks..."
                    )
                } else {
                    DeckSelectorMenu(
                        deckNames = uiState.deckNames,
                        onDeckSelected = viewModel::selectDeck
                    )
                }
            }

            Column(
                modifier = Modifier
                    .verticalScroll(scrollState)
            ) {
                if (uiState.isLoadingFields) {
                    Text("Loading fields...")
                } else {
                    uiState.fieldNames.forEach { fieldName ->
                        ModelTextField(
                            fieldName = fieldName,
                            value = uiState.fieldValues[fieldName] ?: "",
                            onValueChange = { viewModel.setFieldValue(fieldName, it) },
                        )
                    }
                }
            }

            uiState.errorMessage?.let { error ->
                Text(error)
            }
        }
    }
}

@Composable
fun ModelTextField(
    fieldName: String,
    value: String,
    onValueChange: (String) -> Unit,
) {
    Column {
        Text(fieldName)
        TextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
fun DeckSelectorMenu(
    deckNames: List<String>,
    onDeckSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .padding(16.dp)
    ) {
        IconButton(onClick = { expanded = !expanded }) {
            Icon(Icons.Default.MoreVert, contentDescription = "Select deck")
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            deckNames.forEach { deckName ->
                DropdownMenuItem(
                    text = { Text(deckName) },
                    onClick = {
                        onDeckSelected(deckName)
                        expanded = false
                    }
                )
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