package com.example.gsmcompanion

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

data class AnkiConfigsUiState (
    val modelNames: List<String> = emptyList(),
    val selectedModel: String? = null,
    val fieldNames: List<String> = emptyList(),
    val isLoadingModels: Boolean = false,
    val isLoadingFields: Boolean = false,
    val errorMessage: String? = null
    )

class AnkiConfigsViewModel(application : Application) : AndroidViewModel(application) {
    private val client = okhttp3.OkHttpClient()
    private val _uiState = MutableStateFlow(AnkiConfigsUiState())
    val uiState = _uiState.asStateFlow()

    fun selectModel(modelName : String) {
        _uiState.update {
            it.copy (
                selectedModel = modelName
            )
        }
    }

    private suspend fun getAnkiConnectUrl(): String {
        val config = APIConfigs.getConfigs(getApplication()).first()
        return APIConfigs.getURL(config, PortName.AnkiConnect)
    }

    suspend fun getModelNames(){
        val ankiConnectUrl = getAnkiConnectUrl()
        _uiState.update {
            it.copy(
                isLoadingModels = true,
                errorMessage = null

            )
        }
        val body = JSONObject().apply {
            put("action", "modelNames")
            put("version", 5)
        }.toString()
        val request = Request.Builder()
            .url(ankiConnectUrl)
            .post(body.toRequestBody())
            .build()

        withContext(Dispatchers.IO){
            try {
                client.newCall(request).execute().use { response ->
                    val body = response.body.string()
                    val result = JSONObject(body).getJSONArray("result")
                    _uiState.update {
                        it.copy(
                            modelNames = List(result.length()) { i -> result.getString(i) }
                        )
                    }
                }
            } catch (e : Exception) {
                _uiState.update {
                    it.copy(
                        errorMessage = e.message
                    )
                }
            }
            _uiState.update {
                it.copy(
                    isLoadingModels = false
                )
            }
        }
    }

    suspend fun getModelFieldNames(modelName : String){
        val ankiConnectUrl = getAnkiConnectUrl()
        _uiState.update {
            it.copy(
                isLoadingFields = true,
                errorMessage = null
            )
        }
        val body = JSONObject().apply {
            put ("action", "ModelFieldNames")
            put("version", 5)
            put ("params", JSONObject().apply {
                put("modelName", modelName)
            })
        }.toString()
        val request = Request.Builder()
            .url(ankiConnectUrl)
            .post(body.toRequestBody())
            .build()

        withContext(Dispatchers.IO){
            try {
                client.newCall(request).execute().use { response ->
                    val body = response.body.string()
                    val result = JSONObject(body).getJSONArray("result")
                    _uiState.update {
                        it.copy(
                            fieldNames = List(result.length()) { i -> result.getString(i) }
                        )
                    }
                }
            } catch (e : Exception) {
                _uiState.update {
                    it.copy(
                        errorMessage = e.message
                    )
                }
            }
            _uiState.update {
                it.copy(
                    isLoadingFields = false
                )
            }
        }
    }
}