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
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

import kotlinx.coroutines.launch

private val hostnameLabelRegex = Regex("^[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?$")
private val ipv4Regex = Regex("""^((25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)\.){3}(25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)$""")
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
fun LandingScreen(navController: NavController){
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
            DefaultGateway()
            val context = LocalContext.current
            val yomitanPort by APIConfs.getYomitanPort(context)
                .collectAsState(initial = 19633)
            val ankiConnectPort by APIConfs.getAnkiConnectPort(context)
                .collectAsState(initial = 8765)
            val gsmUnifiedPort by APIConfs.getGSMUnifiedPort(context)
                .collectAsState(initial = 7275)
            ConfigPort(
                "Yomitan",
                yomitanPort,
                onSave = { port -> APIConfs.setYomitanPort(context, port) },
            )
            ConfigPort(
                "AnkiConnect",
                ankiConnectPort,
                onSave = { port -> APIConfs.setAnkiConnectPort(context, port) },
            )
            ConfigPort(
                "GSM Unified",
                gsmUnifiedPort,
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
fun DefaultGateway(){
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val savedIP by APIConfs.getDefaultGateway(context)
        .collectAsState(initial = "127.0.0.1")
    var input by remember { mutableStateOf("") }
    LaunchedEffect(savedIP){
        input = savedIP
    }
    val isValid = GatewayCheck(input)
    Column() {
        Text (
            text = "Tailscale/Default Gateway IP ($savedIP)"
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
            Button(onClick = {
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
    onSave: suspend (Int) -> Unit
){
    val scope = rememberCoroutineScope()
    var input by remember { mutableStateOf("") }
    LaunchedEffect(savedPort){
        input = savedPort.toString()
    }
    val isValid = PortCheck(input)
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
            Button(onClick = {
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

fun GatewayCheck(input: String): Boolean {
    val host = input.trim() // Remove whitespace
    
    if (host.isEmpty()) return false
    if (host.length > 253) return false

    if (
        host.contains("://") ||
        host.contains(":") ||
        host.contains("/") ||
        host.contains("?") ||
        host.contains("#") ||
        host.contains("@") ||
        host.any { it.isWhitespace() }
    ) return false

    if (ipv4Regex.matches(host)) return true
    if (host == "localhost") return true

    val labels = host.split(".")
    if (labels.any { it.isEmpty() }) return false

    return labels.all { label ->
        hostnameLabelRegex.matches(label)
    }
}

fun PortCheck(input: String): Boolean {
    val port = input.toIntOrNull()
    if (port == null) return false
    return port in 0..65535
}