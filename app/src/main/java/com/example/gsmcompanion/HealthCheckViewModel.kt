package com.example.gsmcompanion

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import kotlin.time.Duration.Companion.milliseconds

class HealthCheckViewModel(application: Application) : AndroidViewModel(application) {
    private val client = OkHttpClient()
    private val _portHealthStatuses =
        MutableStateFlow(PortHealthStatuses())
    val portHealthStatuses = _portHealthStatuses.asStateFlow()

    val configs = APIConfigs.getConfigs(application).stateIn(
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
                    val tasks = PortName.entries.map { name ->
                        async {
                            val url = APIConfigs.getURL(config, name)
                            checkHealthStatus(url, name)
                        }
                    }
                    withTimeoutOrNull(5000L.milliseconds) {
                        tasks.awaitAll()
                    }
                    delay(2000L.milliseconds)
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
                    key, if (response.code == 200 || (key == PortName.Yomitan && response.code == 405)) ConnectionStatus.Connected else
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