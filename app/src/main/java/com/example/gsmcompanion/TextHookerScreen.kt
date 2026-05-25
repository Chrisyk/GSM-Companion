package com.example.gsmcompanion

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

@Composable
fun TextHookerScreen(navController : NavController){
    val context =LocalContext.current
    val defaultGateway = APIConfs.getDefaultGateway(context).collectAsState(
        initial = "",
    )
    val gsmUnifiedPort = APIConfs.getGSMUnifiedPort(context).collectAsState(
        initial= "",
    )
    val websocketAddress = "ws://$defaultGateway:$gsmUnifiedPort"
    val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS) // keep the socket open indefinitely
        .build()

    val request = Request.Builder()
        .url(websocketAddress) // echo.websocket.org is dead — use this one
        .build()

    val listener = TextHookerWebSocketClient()
    client.newWebSocket(request, listener)
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            BottomAppBar { LandingScreenButton (onClick = { navController.navigate("landing")}) }
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
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(100) { index ->
                    Text("Lazy Item $index", modifier = Modifier.padding(16.dp))
                }
            }
        }
    }
}

@Composable
fun LandingScreenButton(onClick: () -> Unit) {
    Button(onClick = onClick) { Text("Settings") }
}