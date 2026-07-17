package com.example.gsmcompanion

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

enum class CheckCodes {
    SUCCESS,
    NULL_CONVERSION,
    INVALID_RANGE,
    EMPTY_INPUT,
    INVALID_LENGTH,
    INVALID_CHARACTERS,
    INVALID_FORMAT
}

class LandingViewModel(application: Application) : AndroidViewModel(application) {
    private val hostnameLabelRegex = Regex("^[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?$")
    private val ipv4Regex =
        Regex("""^((25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)\.){3}(25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)$""")
    val settingsState: StateFlow<Configs> = APIConfigs.getConfigs(application)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = Configs()
        )

    suspend fun setDefaultGateway(ip: String) {
        APIConfigs.setDefaultGateway(getApplication(), ip)
    }

    suspend fun setGSMUnifiedPort(port: String) {
        val portInt = port.toInt()
        APIConfigs.setGSMUnifiedPort(getApplication(), portInt)
    }

    suspend fun setYomitanPort(port: String) {
        val portInt = port.toInt()
        APIConfigs.setYomitanPort(getApplication(), portInt)
    }

    suspend fun setAnkiConnectPort(port: String) {
        val portInt = port.toInt()
        APIConfigs.setAnkiConnectPort(getApplication(), portInt)
    }

    fun gatewayCheck(input: String): CheckCodes {
        val host = input.trim() // Remove whitespace

        if (host.isEmpty()) return CheckCodes.EMPTY_INPUT
        if (host.length > 253) return CheckCodes.INVALID_LENGTH

        if (
            host.contains("://") ||
            host.contains(":") ||
            host.contains("/") ||
            host.contains("?") ||
            host.contains("#") ||
            host.contains("@") ||
            host.any { it.isWhitespace() }
        ) return CheckCodes.INVALID_CHARACTERS

        if (ipv4Regex.matches(host)) return CheckCodes.SUCCESS
        if (host == "localhost") return CheckCodes.SUCCESS

        val labels = host.split(".")
        if (labels.any { it.isEmpty() }) return CheckCodes.INVALID_FORMAT

        return if (labels.all { label -> hostnameLabelRegex.matches(label) })
            CheckCodes.SUCCESS
        else
            CheckCodes.INVALID_FORMAT
    }

    fun portCheck(input: String): CheckCodes {
        val port = input.toIntOrNull() ?: return CheckCodes.NULL_CONVERSION
        if (port in 1..65535)
            return CheckCodes.SUCCESS
        else
            return CheckCodes.INVALID_RANGE
    }
}