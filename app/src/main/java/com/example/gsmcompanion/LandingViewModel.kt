package com.example.gsmcompanion

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
data class LandingUiState(
    val defaultGateway: String = "127.0.0.1",
    val gsmUnifiedPort: Int = 7275,
    var yomitanPort: Int = 19633,
    var ankiConnectPort: Int = 8765
)

class LandingViewModel(context: Context) : ViewModel() {
    private val hostnameLabelRegex = Regex("^[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?$")
    private val ipv4Regex = Regex("""^((25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)\.){3}(25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)$""")
    val settingsState: StateFlow<LandingUiState> = combine(
        APIConfs.getDefaultGateway(context),
        APIConfs.getGSMUnifiedPort(context),
        APIConfs.getYomitanPort(context),
        APIConfs.getAnkiConnectPort(context),
    ) { gateway, yomitan, anki, gsm ->
        LandingUiState(gateway, yomitan, anki, gsm)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = LandingUiState()
    )

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
        return port in 1..65535
    }

}