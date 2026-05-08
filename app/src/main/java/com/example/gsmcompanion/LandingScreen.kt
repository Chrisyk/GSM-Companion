package com.example.gsmcompanion

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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

        ) {
            Greeting(
                name = "AYN Thor Companion",
            )
            DefaultGateway()
            YomitanPort()
            AnkiConnectPort()

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
    Column() {
        Text (
            text = "Tailscale/Default Gateway IP ($savedIP)"
        )
        Row() {
            TextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f)
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
fun YomitanPort(){
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val savedPort by APIConfs.getYomitanPort(context)
        .collectAsState(initial = 19633)
    var input by remember { mutableStateOf("") }
    LaunchedEffect(savedPort){
        input = savedPort.toString()
    }
    Column() {
        Text (
            text = "Yomitan API port ($savedPort)"
        )
        Row() {
            TextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f)
            )
            Button(onClick = {
                val port = input.toIntOrNull()
                if (port != null) {
                    scope.launch {
                        APIConfs.setYomitanPort(context, port)
                    }
                }
            }) { Text("Set") }
        }
    }
}

@Composable
fun AnkiConnectPort(){
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val savedPort by APIConfs.getAnkiConnectPort(context)
        .collectAsState(initial = 8765)
    var input by remember { mutableStateOf("") }
    LaunchedEffect(savedPort){
        input = savedPort.toString()
    }
    Column() {
        Text (
            text = "Anki Connect API port ($savedPort)"
        )
        Row() {
            TextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f)
            )
            Button(onClick = {
                val port = input.toIntOrNull()
                if (port != null) {
                    scope.launch {
                        APIConfs.setAnkiConnectPort(context, port)
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