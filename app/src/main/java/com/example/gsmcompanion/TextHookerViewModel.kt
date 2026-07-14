package com.example.gsmcompanion

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import kotlinx.serialization.json.JsonElement

data class TextHookerUiState(
    val connectionStatus : ConnectionStatus =
        ConnectionStatus.Disconnected,
    val sentences: List<String> = listOf("今日は散歩しましょう"),
    val termEntriesResponse: TermEntriesResponse? = null,
    val errorMessage: String? = null
)

@Serializable
data class TermEntriesResponse(
    val index: Int,
    val dictionaryEntries: List<DictionaryEntry> = emptyList()
)

@Serializable
data class DictionaryEntry(
    val headwords: List<Headword> = emptyList(),
    val definitions: List<Definition> = emptyList()
)

@Serializable
data class Headword(
    val term: String,
    val reading: String? = null
)

@Serializable
data class Definition(
    val dictionary: String? = null,
    val dictionaryAlias: String? = null,
    val entries: List<DefinitionEntry> = emptyList()
)

@Serializable
data class DefinitionEntry(
    val type: String? = null,
    val content: JsonElement? = null
)

class TextHookerViewModel(application: Application) : AndroidViewModel(application){
    private val textHookerClient = TextHookerClient()
    private val client = okhttp3.OkHttpClient()
    private val _uiState = MutableStateFlow(TextHookerUiState())
    val uiState = _uiState.asStateFlow()
    private var connectionAttemptId = 0
    private var currentUrl: String? = null

    fun connect(url: String) {
        if (currentUrl == url &&
            (_uiState.value.connectionStatus == ConnectionStatus.Connecting ||
                _uiState.value.connectionStatus == ConnectionStatus.Connected)
        ) return

        val attemptId = ++connectionAttemptId
        textHookerClient.close()
        currentUrl = url

        _uiState.update{
            it.copy(
                connectionStatus=
                    ConnectionStatus.Connecting,
                errorMessage = null
            )
        }
        textHookerClient.connect(
            url = url,
            onOpen = {
                if (attemptId == connectionAttemptId) {
                    _uiState.update {
                        it.copy(
                            connectionStatus =
                                ConnectionStatus.Connected
                        )

                    }
                }
            },
            onMessage = { message ->
                if (attemptId == connectionAttemptId) {
                    _uiState.update { state ->
                        state.copy(
                            sentences =
                                (state.sentences + message).takeLast(300)
                        )
                    }
                }
            },
            onClosed = {
                if (attemptId == connectionAttemptId) {
                    _uiState.update {
                        it.copy(
                            connectionStatus =
                                ConnectionStatus.Disconnected
                        )
                    }
                }
            },
            onFailure = { error ->
                if (attemptId == connectionAttemptId) {
                    _uiState.update {
                        it.copy(
                            connectionStatus =
                                ConnectionStatus.Error,
                            errorMessage =
                                error.message
                        )
                    }
                }
            }
        )
    }

    fun disconnect() {
        connectionAttemptId++

        currentUrl = null
        textHookerClient.close()

        _uiState.update {
            it.copy(
                connectionStatus = ConnectionStatus.Disconnected,
                errorMessage = null
            )
        }
    }

    override fun onCleared(){
        disconnect()
    }

    private suspend fun getYomitanUrl(): String {
        val config = APIConfigs.getConfigs(getApplication()).first()
        return APIConfigs.getURL(config, PortName.Yomitan)
    }

    fun onTextPointSelected(sentence: String, charOffset: Int) {
        val subsentence = sentence.substring(charOffset)
        viewModelScope.launch {
            getTermEntries(subsentence)
        }
    }

    suspend fun getTermEntries(subsentence: String) {
        val url = getYomitanUrl()
        val body = JSONObject().apply {
            put("term", subsentence)
        }.toString()
        val request = okhttp3.Request.Builder()
            .url("$url/termEntries")
            .post(body.toRequestBody())
            .build()

        withContext(Dispatchers.IO) {
            try {
                client.newCall(request).execute().use { response ->
                    val responseBody = response.body.string()
                    Log.d("Yomitan", "HTTP ${response.code}")
                    Log.d("Yomitan", "Raw body: $responseBody")
                    val json = Json {
                        ignoreUnknownKeys = true
                        isLenient = true
                    }
                    val parsed = json.decodeFromString<TermEntriesResponse>(responseBody)
                    _uiState.update {
                        it.copy(
                            termEntriesResponse = parsed
                        )
                    }
                    val firstHeadword = parsed.dictionaryEntries
                        .firstOrNull()
                        ?.headwords
                        ?.firstOrNull()


                    Log.d("Yomitan", "term: ${firstHeadword?.term}")
                    Log.d("Yomitan", "reading: ${firstHeadword?.reading}")

                }
            } catch (e: Exception ) {
                Log.e("Yomitan", "Failed to parse term entries", e)

                _uiState.update {
                    it.copy(
                        errorMessage = e.message
                    )
                }
            }
        }

    }

}
