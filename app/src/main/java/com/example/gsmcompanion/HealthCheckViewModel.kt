package com.example.gsmcompanion

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request

enum class PortName {
    GSM,
    Yomitan,
    AnkiConnect,
}

data class PortHealthStatuses (
    val gsmUnifiedPortStatus: ConnectionStatus =
        ConnectionStatus.Disconnected,
    val yomitanPortStatus: ConnectionStatus =
        ConnectionStatus.Disconnected,
    val ankiConnectPortStatus: ConnectionStatus =
        ConnectionStatus.Disconnected,
)

class HealthCheckViewModel(application: Application) : AndroidViewModel(application) {
    private val client = OkHttpClient()
    private val _portHealthStatuses =
        MutableStateFlow(PortHealthStatuses())
    val portHealthStatuses = _portHealthStatuses.asStateFlow() // we have a read-only stream
    // In the Composable we collect it　with collectAsState()
    val defaultGateway = APIConfs.getDefaultGateway(application)
    val gsmUnifiedPort = APIConfs.getGSMUnifiedPort(application)
    val yomitanPort = APIConfs.getYomitanPort(application)
    val ankiConnectPort = APIConfs.getAnkiConnectPort(application)

    init {
        viewModelScope.launch(Dispatchers.IO) {
            while (true) {
                checkHealthStatus(
                    "http://${defaultGateway.first()}:${gsmUnifiedPort.first()}",
                    PortName.GSM
                )
                checkHealthStatus(
                    "http://${defaultGateway.first()}:${yomitanPort.first()}",
                    PortName.Yomitan
                )
                checkHealthStatus(
                    "http://${defaultGateway.first()}:${ankiConnectPort.first()}",
                    PortName.AnkiConnect
                )
                delay(500L)
            }
        }
    }

    // We will use this inside a coroutine with delay, maybe 500 ms
    fun checkHealthStatus(url: String, key : PortName) {
        try {
            val request = Request.Builder()
                .url(url)
                .build()
            client.newCall(request).execute().use { response ->
                updateStatus(
                    key, if (response.code == 200) ConnectionStatus.Connected else
                        ConnectionStatus.Disconnected
                )
            }
        } catch (e : Exception) {
            updateStatus(key, ConnectionStatus.Disconnected)
        }
    }

    fun updateStatus( key : PortName , status: ConnectionStatus) {
        when (key) {
            PortName.GSM -> _portHealthStatuses.update {
                it.copy(
                    gsmUnifiedPortStatus = status
                )
            }

            PortName.Yomitan -> _portHealthStatuses.update {
                it.copy(
                    yomitanPortStatus = status
                )
            }

            PortName.AnkiConnect -> _portHealthStatuses.update {
                it.copy(
                    ankiConnectPortStatus = status
                )
            }
        }
    }


}