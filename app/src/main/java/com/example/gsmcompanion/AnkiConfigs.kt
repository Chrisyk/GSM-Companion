package com.example.gsmcompanion

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONObject

object AnkiFieldStore {
    private fun keyFor(model: String) = stringPreferencesKey("anki_fields_$model")
    private val SELECTED_MODEL = stringPreferencesKey("anki_selected_model")
    suspend fun save(context: Context, model: String, values: Map<String, String>) {
        val json = JSONObject(values as Map<*, *>).toString()
        context.dataStore.edit {
            it[keyFor(model)] = json
            it[SELECTED_MODEL] = model
        }
    }

    fun getSelectedModel(context: Context) : Flow<String?> =
        context.dataStore.data.map { preferences ->
            preferences[SELECTED_MODEL]
        }

    fun getFields(context: Context, model: String) : Flow<Map<String, String>> =
        context.dataStore.data.map { pref ->
            val json = pref[keyFor(model)] ?: return@map emptyMap()
            val obj = JSONObject(json)
            obj.keys().asSequence().associateWith {
                obj.getString(it)
            }
        }
}