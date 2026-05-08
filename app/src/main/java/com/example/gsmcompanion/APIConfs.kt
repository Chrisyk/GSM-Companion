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
object APIConfs {
    private val DEFAULT_GATEWAY = stringPreferencesKey("default_gateway")
    private val YOMITAN_API_PORT = intPreferencesKey("yomitan_api_port")
    private val ANKICONNECT_API_PORT = intPreferencesKey("ankiconnect_api_port")

    fun getDefaultGateway(context: Context): Flow<String> =
        context.dataStore.data.map { prefs ->
            prefs[DEFAULT_GATEWAY] ?: "127.0.0.1"
        }
    suspend fun setDefaultGateway(context: Context, ip: String) {
        context.dataStore.edit { prefs ->
            prefs[DEFAULT_GATEWAY] = ip
        }
    }
    fun getYomitanPort(context: Context): Flow<Int> =
        context.dataStore.data.map { prefs ->
            prefs[YOMITAN_API_PORT] ?: 19633
        }
    suspend fun setYomitanPort(context: Context, port: Int) {
        context.dataStore.edit { prefs ->
            prefs[YOMITAN_API_PORT] = port
        }
    }
    fun getAnkiConnectPort(context: Context): Flow<Int> =
        context.dataStore.data.map { prefs ->
            prefs[ANKICONNECT_API_PORT] ?: 8765
        }
    suspend fun setAnkiConnectPort(context: Context, port: Int) {
        context.dataStore.edit { prefs ->
            prefs[ANKICONNECT_API_PORT] = port
        }
    }
}