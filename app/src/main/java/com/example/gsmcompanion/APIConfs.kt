package com.example.gsmcompanion

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")
enum class PortName {
    GSM,
    Yomitan,
    AnkiConnect,
}
enum class ConnectionStatus {
    Disconnected,
    Connecting,
    Connected,
    Error,
}
data class Configs (
    val defaultGateway : String = "127.0.0.1",
    val gsmUnifiedPort : Int = 7275,
    val yomitanApiPort : Int = 19633,
    val ankiConnectApiPort : Int = 8765,
)
data class PortHealthStatuses (
    val gsmUnifiedPortStatus: ConnectionStatus =
        ConnectionStatus.Disconnected,
    val yomitanPortStatus: ConnectionStatus =
        ConnectionStatus.Disconnected,
    val ankiConnectPortStatus: ConnectionStatus =
        ConnectionStatus.Disconnected,
)

object APIConfs {
    private val DEFAULT_GATEWAY = stringPreferencesKey("default_gateway")
    private val YOMITAN_API_PORT = intPreferencesKey("yomitan_api_port")
    private val ANKICONNECT_API_PORT = intPreferencesKey("ankiconnect_api_port")
    private val GSM_UNIFIED_PORT = intPreferencesKey("gsm_unified_port")
    private val Configs = Configs()
    fun getConfigs(context: Context): Flow<Configs> =
        context.dataStore.data.map { prefs ->
            Configs(
                defaultGateway = prefs[DEFAULT_GATEWAY] ?: Configs.defaultGateway,
                gsmUnifiedPort = prefs[GSM_UNIFIED_PORT] ?: Configs.gsmUnifiedPort,
                yomitanApiPort = prefs[YOMITAN_API_PORT] ?: Configs.yomitanApiPort,
                ankiConnectApiPort = prefs[ANKICONNECT_API_PORT] ?: Configs.ankiConnectApiPort,
            )
        }
    fun getURL(
        configs: Configs,
        portName: PortName,
        protocol: String = "http"
    ) : String {
        val port = when (portName) {
            PortName.GSM -> configs.gsmUnifiedPort
            PortName.Yomitan -> configs.yomitanApiPort
            PortName.AnkiConnect -> configs.ankiConnectApiPort
        }
        return "$protocol://${configs.defaultGateway}:$port"
    }
    suspend fun setDefaultGateway(context: Context, ip: String) {
        context.dataStore.edit { prefs ->
            prefs[DEFAULT_GATEWAY] = ip
        }
    }
    suspend fun setYomitanPort(context: Context, port: Int) {
        context.dataStore.edit { prefs ->
            prefs[YOMITAN_API_PORT] = port
        }
    }
    suspend fun setAnkiConnectPort(context: Context, port: Int) {
        context.dataStore.edit { prefs ->
            prefs[ANKICONNECT_API_PORT] = port
        }
    }
    suspend fun setGSMUnifiedPort(context: Context, port: Int) {
        context.dataStore.edit { prefs ->
            prefs[GSM_UNIFIED_PORT] = port
        }
    }
}