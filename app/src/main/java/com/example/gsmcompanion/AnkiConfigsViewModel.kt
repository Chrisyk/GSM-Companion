package com.example.gsmcompanion

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import kotlin.collections.emptyMap

data class AnkiConfigsUiState (
    val modelNames: List<String> = emptyList(),
    val selectedModel: String? = null,
    val fieldNames: List<String> = emptyList(),
    val fieldValues: Map<String, String> = emptyMap(),
    val isLoadingModels: Boolean = false,
    val isLoadingFields: Boolean = false,
    val errorMessage: String? = null
    )

class AnkiConfigsViewModel(application : Application) : AndroidViewModel(application) {
    private val client = okhttp3.OkHttpClient()
    private val _uiState = MutableStateFlow(AnkiConfigsUiState())
    val uiState = _uiState.asStateFlow()
    private var fieldJob: Job? = null
    private var modelFetchAttemptId = 0
    private var fieldFetchAttemptId = 0

    suspend fun saveFields() {
        val model = _uiState.value.selectedModel ?: return
        AnkiFieldStore.save(getApplication(), model, _uiState.value.fieldValues)
    }

    fun loadFields(modelName: String) {
        fieldJob?.cancel()
        fieldJob = viewModelScope.launch {
            val fields = AnkiFieldStore.getFields(getApplication(), modelName).first()
            if (_uiState.value.selectedModel == modelName) {
                _uiState.update {
                    it.copy(
                        fieldValues = fields
                    )
                }
            }
        }
    }

    fun selectModel(modelName : String) {
        _uiState.update {
            it.copy (
                selectedModel = modelName
            )
        }
    }

    fun setFieldValue(fieldName: String, value: String) {
        _uiState.update {
            it.copy(
                fieldValues = it.fieldValues + (fieldName to value)
            )
        }
    }

    private suspend fun getAnkiConnectUrl(): String {
        val config = APIConfigs.getConfigs(getApplication()).first()
        return APIConfigs.getURL(config, PortName.AnkiConnect)
    }

    private suspend fun getYomitanUrl(): String {
        val config = APIConfigs.getConfigs(getApplication()).first()
        return APIConfigs.getURL(config, PortName.Yomitan)
    }

    suspend fun getModelNames(){
        val ankiConnectUrl = getAnkiConnectUrl()
        val currentModelFetchAttemptId = ++modelFetchAttemptId
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
                    val selectedModel = AnkiFieldStore.getSelectedModel(getApplication()).first()
                    if (currentModelFetchAttemptId == modelFetchAttemptId) {
                        _uiState.update {
                            it.copy(
                                modelNames = List(result.length()) { i -> result.getString(i) },
                                selectedModel = selectedModel
                            )
                        }
                    }
                }
            } catch (e : Exception) {
                if (currentModelFetchAttemptId == modelFetchAttemptId) {
                    _uiState.update {
                        it.copy(
                            errorMessage = e.message
                        )
                    }
                }
            }
            if (currentModelFetchAttemptId == modelFetchAttemptId) {
                _uiState.update {
                    it.copy(
                        isLoadingModels = false
                    )
                }
            }
        }
    }

    suspend fun getFieldNames(modelName : String){
        val ankiConnectUrl = getAnkiConnectUrl()
        val currentFieldFetchAttemptId = ++fieldFetchAttemptId
        _uiState.update {
            it.copy(
                isLoadingFields = true,
                errorMessage = null
            )
        }
        val body = JSONObject().apply {
            put ("action", "modelFieldNames")
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
                    if (fieldFetchAttemptId == currentFieldFetchAttemptId) {
                        _uiState.update {
                            it.copy(
                                fieldNames = List(result.length()) { i -> result.getString(i) }
                            )
                        }
                    }
                }
            } catch (e : Exception) {
                if (fieldFetchAttemptId == currentFieldFetchAttemptId) {
                    _uiState.update {
                        it.copy(
                            errorMessage = e.message
                        )
                    }
                }
            }
            if (fieldFetchAttemptId == currentFieldFetchAttemptId) {
                _uiState.update {
                    it.copy(
                        isLoadingFields = false
                    )
                }
            }
        }
    }

    suspend fun getFieldValuesYomitan() {
        val modelNames = _uiState.value.modelNames
        val selectedModel = _uiState.value.selectedModel ?: return
        val knownFields = _uiState.value.fieldNames.toSet()
        val url = getYomitanUrl()
        val body = JSONObject().apply {
            put("profileIndex", 0)
        }.toString()
        val request = Request.Builder()
            .url("$url/ankiCardFormats")
            .post(body.toRequestBody())
            .build()
        if (modelNames.isEmpty()) {
            return
        }
        withContext(Dispatchers.IO){
            try {
                client.newCall(request).execute().use { response ->
                    val body = response.body.string()
                    val models = JSONArray(body)

                    for (i in 0 until models.length()) {
                        val model = models.getJSONObject(i)
                        val modelName = model.getString("model")

                        if (modelName != selectedModel) {
                            continue
                        }

                        val fieldsObj = model.getJSONObject("fields")
                        val matched = fieldsObj.keys().asSequence()
                            .filter { it in knownFields }
                            .associateWith { fieldName ->
                                fieldsObj.getJSONObject(fieldName).getString("value")
                            }
                        if (_uiState.value.selectedModel == selectedModel) {
                            _uiState.update {
                                it.copy(
                                    fieldValues = it.fieldValues + matched
                                )
                            }
                        }
                        break
                    }
                }
            } catch (e: Exception) {
                if (_uiState.value.selectedModel == selectedModel) {
                    _uiState.update { it.copy(errorMessage = e.message) }
                }
            }
        }

    }

}