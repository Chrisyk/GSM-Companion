package com.example.gsmcompanion

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController

@Composable
fun HealthCheckScreen(
    navController: NavController,
    viewModel: HealthCheckViewModel = viewModel()
) {
    // What exactly is the by keyword and why does '=' not work here?
    val state by viewModel.portHealthStatuses.collectAsState()
    val scrollState = rememberScrollState()
    DisposableEffect(Unit) {
        viewModel.startPinging()
        onDispose {
            viewModel.stopPinging()
        }
    }
    Scaffold(
        Modifier.fillMaxSize(),
        bottomBar = {
            BottomAppBar {
                BottomBar(navController)
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(scrollState)
        ) {
            HealthStatus("GSM Unified Port", state.gsmUnifiedPortStatus)
            HealthStatus("Yomitan Port", state.yomitanPortStatus)
            HealthStatus("Anki Connect Port", state.ankiConnectPortStatus)
        }
    }
}

@Composable
fun HealthStatus(name: String, status: ConnectionStatus) {
    val statusStr = if (status == ConnectionStatus.Connected) {
        "connected"
    } else "disconnected"
    Text(
        text = "$name : $statusStr"
    )
}