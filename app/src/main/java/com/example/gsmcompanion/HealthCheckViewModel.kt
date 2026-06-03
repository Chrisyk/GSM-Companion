package com.example.gsmcompanion

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request

class HealthCheckViewModel(application: Application) : AndroidViewModel(application) {
    private val client = OkHttpClient()
    private val _portHealthStatuses =
        MutableStateFlow(PortHealthStatuses())
    val portHealthStatuses = _portHealthStatuses.asStateFlow()

    val configs = APIConfs.getConfigs(application).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = Configs()
    )
    private var pingJob : Job? = null

    fun startPinging() {
        if (pingJob != null) return

        pingJob = viewModelScope.launch(Dispatchers.IO) {
            configs.collectLatest { config ->
                while (isActive) {
                    PortName.entries.forEach { name ->
                        val url = APIConfs.getURL(config, name)
                        launch {
                            checkHealthStatus(url, name)
                        }
                    }
                    delay(2000L)
                }
            }
        }
    }

    fun stopPinging() {
        pingJob?.cancel()
        pingJob = null
    }


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