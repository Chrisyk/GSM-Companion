package com.example.gsmcompanion

import android.R.attr.onClick
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@Composable
fun TextHookerScreen(navController : NavController){
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
