package com.example.gsmcompanion

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.gsmcompanion.ui.theme.GSMCompanionTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GSMCompanionTheme {
                val navController = rememberNavController()
                NavHost(navController, startDestination = "landing") {
                    composable("landing") { LandingScreen(navController) }
                    composable("textHooker") { TextHookerScreen(navController) }
                }
            }
        }
    }
}

@Composable
fun LandingScreen(
    navController: NavController,
    viewModel: LandingViewModel = viewModel()
){
    val state by viewModel.settingsState.collectAsState()
    val scrollState = rememberScrollState()
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            BottomAppBar { TextHookerPageButton(onClick = { navController.navigate("textHooker") }) }
        }

    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(scrollState)

        ) {
            Greeting(
                name = "AYN Thor Companion",
            )
            DefaultGateway(state.defaultGateway)
            val context = LocalContext.current
            ConfigPort(
                "Yomitan",
                state.yomitanPort,
                onSave = { port -> APIConfs.setYomitanPort(context, port) },
            )
            ConfigPort(
                "AnkiConnect",
                state.ankiConnectPort,
                onSave = { port -> APIConfs.setAnkiConnectPort(context, port) },
            )
            ConfigPort(
                "GSM Unified",
                state.gsmUnifiedPort,
                onSave = { port -> APIConfs.setGSMUnifiedPort(context, port) },
            )
        }

    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Welcome to the $name. We will need a couple of things to get started:",
        modifier = modifier
    )
}

@Composable
fun TextHookerPageButton(onClick: () -> Unit) {
    Button(onClick = onClick) { Text("Text Hooker") }
}

@Composable
fun DefaultGateway(
    savedIp: String,
    viewModel: LandingViewModel = viewModel()
){
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var input by remember { mutableStateOf("") }
    LaunchedEffect(savedIp){
        input = savedIp
    }
    val isValid = viewModel.GatewayCheck(input)
    Column() {
        Text (
            text = "Tailscale/Default Gateway IP ($savedIp)"
        )
        Row() {
            TextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                isError = input.isNotBlank() && !isValid,
                supportingText = {
                    if (input.isNotBlank() && !isValid) {
                        Text("Enter hostname/IP only, no scheme, port, or path")
                    }
                }
            )
            Button(
                enabled = isValid,
                onClick = {
                if (input.isNotBlank()) {
                    scope.launch {
                        APIConfs.setDefaultGateway(context, input)
                    }
                }
            }) { Text("Set") }
        }
    }
}

@Composable
fun ConfigPort(
    label: String,
    savedPort: Int,
    onSave: suspend (Int) -> Unit,
    viewModel: LandingViewModel = viewModel()
){
    val scope = rememberCoroutineScope()
    var input by remember { mutableStateOf("") }
    LaunchedEffect(savedPort){
        input = savedPort.toString()
    }
    val isValid = viewModel.PortCheck(input)
    Column() {
        Text (
            text = "$label API port ($savedPort)"
        )
        Row() {
            TextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                isError = input.isNotBlank() && !isValid,
                supportingText = {
                    if (input.isNotBlank() && !isValid) {
                        Text("Enter a valid port between 0-65535")
                    }
                }
            )
            Button(
                enabled = isValid,
                onClick = {
                val port = input.toIntOrNull()
                if (port != null) {
                    scope.launch {
                        onSave(port)
                    }
                }
            }) { Text("Set") }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    GSMCompanionTheme {
        Greeting("AYN Thor Companion")
    }
}