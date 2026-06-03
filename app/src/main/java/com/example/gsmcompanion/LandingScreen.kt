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
                    composable("healthCheck") { HealthCheckScreen(navController) }
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
            BottomBar(navController)
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
            ConfigSettings(
                "Default Gateway",
                state.defaultGateway,
                onSave = { gateway -> viewModel.setDefaultGateway(gateway) },
                { input : String -> viewModel.gatewayCheck(input) }
            )
            ConfigSettings (
                "Yomitan",
                state.yomitanApiPort.toString(),
                onSave = { port -> viewModel.setYomitanPort(port) },
                { input : String -> viewModel.portCheck(input) }
            )
            ConfigSettings(
                "AnkiConnect",
                state.ankiConnectApiPort.toString(),
                onSave = { port -> viewModel.setAnkiConnectPort(port) },
                { input : String -> viewModel.portCheck(input) }
            )
            ConfigSettings(
                "GSM Unified",
                state.gsmUnifiedPort.toString(),
                onSave = { port : String -> viewModel.setGSMUnifiedPort(port) },
                { input : String -> viewModel.portCheck(input) }
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

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    GSMCompanionTheme {
        Greeting("AYN Thor Companion")
    }
}

@Composable
fun ConfigSettings(
    label: String,
    savedConfig: String,
    onSave: suspend (String) -> Unit,
    check: (String) -> CheckCodes
){
    val scope = rememberCoroutineScope()
    var input by remember { mutableStateOf("") }
    LaunchedEffect(savedConfig){
        input = savedConfig
    }
    val code = check(input)
    val isValid = code == CheckCodes.SUCCESS
    Column {
        Text (
            text = "$label: ($savedConfig)"
        )
        Row {
            TextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                isError = input.isNotBlank() && !isValid,
                supportingText = {
                    if (input.isNotBlank()) {
                        val message = when (code) {
                            CheckCodes.SUCCESS -> ""
                            CheckCodes.NULL_CONVERSION -> "Must be a number"
                            CheckCodes.INVALID_RANGE -> "Port must be 1-65535"
                            CheckCodes.EMPTY_INPUT -> "Cannot be empty"
                            CheckCodes.INVALID_LENGTH -> "Too long"
                            CheckCodes.INVALID_CHARACTERS -> "Invalid characters"
                            CheckCodes.INVALID_FORMAT -> "Invalid format"
                        }
                        if (message.isNotEmpty()) Text(message)
                    }
                }
            )
            Button(
                enabled = isValid,
                onClick = {
                    scope.launch {
                        onSave(input)
                    }
            }) { Text("Set") }
        }
    }
}

@Composable
fun BottomBar(navController: NavController){
    BottomAppBar {
        TextHookerPageButton{ navController.navigate("textHooker") }
        HealthCheckPageButton { navController.navigate("healthCheck") }
        LandingScreenButton { navController.navigate("landing") }
    }
}

@Composable
fun TextHookerPageButton(onClick: () -> Unit) {
    Button(onClick = onClick) { Text("Text Hooker") }
}

@Composable
fun HealthCheckPageButton(onClick: () -> Unit) {
    Button(onClick = onClick) { Text ("Health Check") }
}

@Composable
fun LandingScreenButton(onClick: () -> Unit) {
    Button(onClick = onClick) { Text("Settings") }
}