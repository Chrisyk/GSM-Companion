package com.example.gsmcompanion

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController

/*
    TextHookerScreen.kt
        Pure Compose UI.
        Reads uiState.
        Displays sentence list and connection status.

    TextHookerViewModel.kt
        Owns connection lifecycle.
        Builds URL from settings.
        Starts/stops websocket.
        Stores received messages in StateFlow.

    TexthookerClient.kt
        Thin OkHttp wrapper.
        Knows how to connect, receive, fail, close.
        Does not know about Compose.

    APIConfs.kt / SettingsStore.kt
        Stores host and port.
     */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextHookerScreen(
    navController : NavController,
    viewModel: TextHookerViewModel = viewModel()
    ) {
    val uiState by viewModel.uiState.collectAsState()
    val gsmUnifiedPort by APIConfs.getGSMUnifiedPort(LocalContext.current)
        .collectAsState(initial=7275)
    val defaultGateway by APIConfs.getDefaultGateway(LocalContext.current)
        .collectAsState(initial="127.0.0.1")
    val websocketAddress = "ws://$defaultGateway:$gsmUnifiedPort"
    LaunchedEffect(websocketAddress) {
        viewModel.connect(websocketAddress)
    }
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "$websocketAddress: ${viewModel.statusMessage()}"
                    )
                }
            )
        },
        bottomBar = {
            BottomAppBar { LandingScreenButton (onClick = { navController.navigate("landing")}) }
        }

    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)

        ) {
            MessageList(uiState.sentences)
        }
    }
}

@Composable
fun MessageList(sentences: List<String>) {
    val listState = rememberLazyListState()
    LaunchedEffect(sentences.size) {
        if (sentences.isNotEmpty()) {
            listState.animateScrollToItem(sentences.size -1)
        }
    }
    LazyColumn (
        state = listState,
        modifier = Modifier.fillMaxSize()
    ) {
        items(
            items = sentences,
            key = { it.hashCode() + Math.random()}
        ) { sentence ->
            Text(
                text = sentence,
                modifier = Modifier.padding(8.dp)
            )

        }
    }
}

@Composable
fun LandingScreenButton(onClick: () -> Unit) {
    Button(onClick = onClick) { Text("Settings") }
}