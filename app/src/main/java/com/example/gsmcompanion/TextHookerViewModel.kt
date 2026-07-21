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
import kotlinx.serialization.json.JsonElement
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

data class TextHookerUiState(
    val connectionStatus: ConnectionStatus =
        ConnectionStatus.Disconnected,
    val sentences: List<String> = emptyList(),
    val textHookerErrorMessage: String? = null,
    val termEntriesResponse: TermEntriesResponse? = null,
    val termEntriesErrorMessage: String? = null,
    val ankiFieldsMessage: String? = null,
    val duplicateTermsIds: Map<String, Long> = emptyMap(),
    val selectedSentenceIndex: Int? = null,
    val offset: Int? = null,
    val originalTextLength: Int? = null,
    val addingTerm: String? = null
)

private data class SelectionContext(
    val originalSentence: String? = null,
)

@Serializable
data class AnkiFieldsResponse(
    val fields: List<Map<String, String>> = emptyList(),
    val audioMedia: List<AudioMedia> = emptyList(),
    val dictionaryMedia: List<DictionaryMedia> = emptyList()
)

@Serializable
data class DictionaryMedia(
    val dictionary: String? = null,
    val path: String? = null,
    val mediaType: String? = null,
    val content: String? = null,
    val ankiFilename: String? = null
)

@Serializable
data class AudioMedia(
    val content: String? = null,
    val ankiFilename: String? = null
)

@Serializable
data class TermEntriesResponse(
    val index: Int = 0,
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
    val reading: String? = null,
    val sources: List<Source> = emptyList()
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

@Serializable
data class Source(
    val originalText: String? = null,
)

class TextHookerViewModel(application: Application) : AndroidViewModel(application) {
    private val textHookerClient = TextHookerClient()
    private val client = okhttp3.OkHttpClient()
    private val _uiState = MutableStateFlow(TextHookerUiState())
    val uiState = _uiState.asStateFlow()
    private var connectionAttemptId = 0
    private var termsRequestId = 0
    private var currentUrl: String? = null
    private var selectionContext: SelectionContext? = null
    fun connect(url: String) {
        if (currentUrl == url &&
            (_uiState.value.connectionStatus == ConnectionStatus.Connecting ||
                    _uiState.value.connectionStatus == ConnectionStatus.Connected)
        ) return

        val attemptId = ++connectionAttemptId
        textHookerClient.close()
        currentUrl = url

        _uiState.update {
            it.copy(
                connectionStatus =
                    ConnectionStatus.Connecting,
                textHookerErrorMessage = null
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
                            textHookerErrorMessage =
                                error.message
                        )
                    }
                }
            }
        )
    }

    fun clearTermEntries() {
        _uiState.update {
            it.copy(
                selectedSentenceIndex = null,
                termEntriesResponse = null,
                termEntriesErrorMessage = null,
                offset = null,
                originalTextLength = null,
                duplicateTermsIds = emptyMap()
            )
        }
    }

    fun disconnect() {
        connectionAttemptId++

        currentUrl = null
        textHookerClient.close()

        _uiState.update {
            it.copy(
                connectionStatus = ConnectionStatus.Disconnected,
                textHookerErrorMessage = null
            )
        }
    }

    override fun onCleared() {
        disconnect()
    }

    private suspend fun getYomitanUrl(): String {
        val config = APIConfigs.getConfigs(getApplication()).first()
        return APIConfigs.getURL(config, PortName.Yomitan)
    }

    private suspend fun getAnkiConnectUrl(): String {
        val config = APIConfigs.getConfigs(getApplication()).first()
        return APIConfigs.getURL(config, PortName.AnkiConnect)
    }

    fun onTextPointSelected(selectedSentenceIndex: Int?, sentence: String, charOffset: Int) {
        val subsentence = sentence.substring(charOffset)

        selectionContext = SelectionContext(
            originalSentence = sentence,
        )

        _uiState.update {
            it.copy(
                selectedSentenceIndex = selectedSentenceIndex,
                offset = charOffset,
                originalTextLength = 1
            )
        }

        viewModelScope.launch {
            getTermEntries(subsentence)
        }
    }

    suspend fun getTermEntries(subsentence: String) {
        val requestId = ++termsRequestId
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

                    val json = Json {
                        ignoreUnknownKeys = true
                        isLenient = true
                    }
                    val parsed = json.decodeFromString<TermEntriesResponse>(responseBody)

                    val originalText = parsed.dictionaryEntries
                        .firstOrNull()
                        ?.headwords
                        ?.firstOrNull()
                        ?.sources
                        ?.firstOrNull()?.originalText

                    if (originalText != null) {
                        _uiState.update {
                            it.copy(originalTextLength = originalText.length)
                        }
                    }

                    _uiState.update {
                        it.copy(
                            termEntriesResponse = parsed,
                            termEntriesErrorMessage = null,
                            duplicateTermsIds = emptyMap()
                        )
                    }

                    getExistingNotesId(requestId)
                }
            } catch (e: Exception) {
                Log.e("Yomitan", "Failed to parse term entries", e)

                _uiState.update {
                    it.copy(
                        termEntriesResponse = null,
                        termEntriesErrorMessage = e.message ?: "Failed to load definition"
                    )
                }
            }
        }
    }

    fun onClickCardToAnki(term: String) {
        _uiState.update {
            it.copy(
                ankiFieldsMessage = null,
                addingTerm = term
            )
        }
        viewModelScope.launch {
            val selectedModel =
                AnkiFieldStore.getSelectedModel(getApplication()).first() ?: return@launch
            val fieldsMap: Map<String, String> =
                AnkiFieldStore.getFields(getApplication(), selectedModel).first()
            try {
                val ankiFields = getMarkerAnkiFields(term, fieldsMap)
                val fields = buildAnkiFields(fieldsMap, ankiFields)

                ankiFields.audioMedia.forEach {
                    if (it.ankiFilename == null || it.content == null) return@forEach
                    storeMediaFile(it.ankiFilename, it.content)
                }
                ankiFields.dictionaryMedia.forEach {
                    if (it.ankiFilename == null || it.content == null) return@forEach
                    storeMediaFile(it.ankiFilename, it.content)
                }

                val noteId = addNote(fields)
                _uiState.update {
                    it.copy(duplicateTermsIds = it.duplicateTermsIds + mapOf(term to noteId))
                }

            } catch (e: Exception) {
                Log.e("Yomitan", "Failed to resolve Anki fields", e)
                _uiState.update {
                    it.copy(
                        ankiFieldsMessage = e.message ?: "Failed to resolve Anki fields"
                    )
                }
            } finally {
                _uiState.update { it.copy(addingTerm = null) }
            }

        }
    }

    suspend fun addNote(ankiFields: Map<String, String>): Long {
        val url = getAnkiConnectUrl()
        val selectedDeckName = AnkiFieldStore.getSelectedDeck(getApplication()).first()
            ?: throw IllegalStateException("No Anki deck selected")
        val selectedModelName = AnkiFieldStore.getSelectedModel(getApplication()).first()
            ?: throw IllegalStateException("No Anki note type selected")
        val note = JSONObject().apply {
            put("deckName", selectedDeckName)
            put("modelName", selectedModelName)
            put("fields", JSONObject(ankiFields))
            put("options", JSONObject().apply {
                put("allowDuplicate", true)
            })
            put("tags", JSONArray().put("GSMCompanion"))
        }
        val body = JSONObject().apply {
            put("action", "addNote")
            put("version", 5)
            put("params", JSONObject().put("note", note))
        }.toString()

        val request = okhttp3.Request.Builder()
            .url(url)
            .post(body.toRequestBody())
            .build()

        return withContext(Dispatchers.IO) {
            client.newCall(request).execute().use { response ->
                val body = JSONObject(response.body.string())
                val error = body.opt("error")
                Log.d("Anki", "error: $error")
                if (error != null && error != JSONObject.NULL) {
                    throw IllegalStateException("addNote Failed $error")
                }
                val noteId = body.getLong("result")
                _uiState.update {
                    it.copy(
                        ankiFieldsMessage = "Note Added Successfully: $noteId"
                    )
                }
                noteId
            }
        }

    }

    private val markerPattern = Regex("""\{([\p{L}\p{N}_-]+)\}""")

    fun buildAnkiFields(fieldsMap: Map<String, String>, ankiFields: AnkiFieldsResponse)
            : Map<String, String> {
        val rendered = ankiFields.fields.firstOrNull() ?: return emptyMap()

        val originalSentence = selectionContext?.originalSentence
            ?: throw IllegalStateException("Failed to load sentence")
        val offset = _uiState.value.offset
            ?: throw IllegalStateException("Failed to load target offset")
        val originalTextLength = _uiState.value.originalTextLength
            ?: throw IllegalStateException("Failed to load target length")

        val localMarkers = mapOf(
            "sentence" to originalSentence,
            "cloze-prefix" to originalSentence.substring(0, offset),
            "cloze-body" to originalSentence.substring(offset, offset + originalTextLength),
            "cloze-suffix" to originalSentence.substring(offset + originalTextLength)
        )

        val merged = rendered + localMarkers
        return fieldsMap.mapValues { (_, template) ->
            markerPattern.replace(template) { match ->
                merged[match.groupValues[1]] ?: ""
            }
        }
    }

    suspend fun getMarkerAnkiFields(
        term: String,
        fieldsMap: Map<String, String>
    ): AnkiFieldsResponse {
        val url = getYomitanUrl()

        val markers = parseMarkers(fieldsMap)
        val body = JSONObject().apply {
            put("text", term)
            put("type", "term")
            put("markers", markers)
            put("maxEntries", 2)
            put("includeMedia", true)
        }.toString()

        val request = okhttp3.Request.Builder()
            .url("$url/ankiFields")
            .post(body.toRequestBody())
            .build()

        return withContext(Dispatchers.IO) {
            client.newCall(request).execute().use { response ->
                val responseBody = response.body.string()
                val json = Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                }
                json.decodeFromString<AnkiFieldsResponse>(responseBody)
            }
        }
    }

    private suspend fun storeMediaFile(filename: String, base64Content: String) {
        val ankiConnectUrl = getAnkiConnectUrl()
        val body = JSONObject().apply {
            put("action", "storeMediaFile")
            put("version", 5)
            put("params", JSONObject().apply {
                put("filename", filename)
                put("data", base64Content)
            })
        }.toString()

        val request = okhttp3.Request.Builder()
            .url(ankiConnectUrl)
            .post(body.toRequestBody())
            .build()
        withContext(Dispatchers.IO) {
            client.newCall(request).execute().use { response ->
                val error = JSONObject(response.body.string()).opt("error")
                if (error != null && error != JSONObject.NULL) {
                    throw IllegalStateException("storeMediaFile Failed: $error")
                }
            }
        }
    }

    private suspend fun getFirstFieldName(model: String): String {
        val body = JSONObject().apply {
            put("action", "modelFieldNames")
            put("version", 6)
            put("params", JSONObject().put("modelName", model))
        }.toString()
        val request = okhttp3.Request.Builder()
            .url(getAnkiConnectUrl()).post(body.toRequestBody()).build()
        return withContext(Dispatchers.IO) {
            client.newCall(request).execute().use { response ->
                val result = JSONObject(response.body.string()).optJSONArray("result")
                    ?: throw IllegalStateException("Could not get field names")
                if (result.length() == 0) throw IllegalStateException("Model has no fields")
                result.getString(0)
            }
        }
    }

    fun getExistingNotesId(requestId: Int = termsRequestId) {
        viewModelScope.launch {
            try {
                val selectedModel = AnkiFieldStore.getSelectedModel(getApplication()).first()
                    ?: throw IllegalStateException("No Anki model selected")
                val key = AnkiFieldStore.getFirstField(getApplication(), selectedModel).first()
                    ?: getFirstFieldName(selectedModel)

                val duplicateTermsIds = findDuplicateTermsIds(key)

                if (requestId == termsRequestId) {
                    _uiState.update {
                        it.copy(
                            duplicateTermsIds = it.duplicateTermsIds + duplicateTermsIds
                        )
                    }
                }


            } catch (e: Exception) {
                Log.d("Anki", "Failed to fetch existing notes id")
                _uiState.update {
                    it.copy(
                        ankiFieldsMessage = e.message
                    )
                }
            }
        }

    }

    suspend fun findDuplicateTermsIds(key: String): Map<String, Long> {
        val terms = _uiState.value.termEntriesResponse?.dictionaryEntries?.mapNotNull { hw ->
            hw.headwords.firstOrNull()?.term
        } ?: throw IllegalStateException("No Terms Found")
        val duplicatesMap: MutableMap<String, Long> = mutableMapOf()

        val actions = JSONArray().apply {
            terms.forEach { term ->
                put(JSONObject().apply {
                    put("action", "findNotes")
                    put(
                        "params", JSONObject().put(
                            "query", "$key:\"$term\""
                        )
                    )
                })
            }
        }

        val body = JSONObject().apply {
            put("action", "multi")
            put("version", 6)
            put("params", JSONObject().put("actions", actions))
        }.toString()

        val request = okhttp3.Request.Builder()
            .url(getAnkiConnectUrl())
            .post(body.toRequestBody())
            .build()

        withContext(Dispatchers.IO) {
            client.newCall(request).execute().use { response ->
                val responseBody = response.body.string()
                val result = JSONObject(responseBody).optJSONArray("result") ?: return@use
                List(result.length()) { index ->
                    val ids = result.optJSONArray(index)
                    if (ids != null && ids.length() > 0) {
                        duplicatesMap[terms[index]] = ids.getLong(0)
                    }
                }
            }
        }
        return duplicatesMap
    }

    fun openAnkiCard(noteId: Long) {
        viewModelScope.launch {
            val body = JSONObject().apply {
                put("action", "guiBrowse")
                put("version", 6)
                put("params", JSONObject().put("query", "nid:$noteId"))
            }.toString()

            val request = okhttp3.Request.Builder()
                .url(getAnkiConnectUrl())
                .post(body.toRequestBody())
                .build()
            Log.d("Anki GUI request body: ", body)
            try {
                withContext(Dispatchers.IO) {
                    client.newCall(request).execute().use { response ->
                        val responseBody = response.body.string()
                        val error = JSONObject(responseBody).opt("error")
                        if (error != null && error != JSONObject.NULL) {
                            throw IllegalStateException("guiBrowse Failed: $error")
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        ankiFieldsMessage = e.message ?: "Failed to open Anki note"
                    )
                }
            }
        }
    }

    private fun parseMarkers(fieldsMap: Map<String, String>): JSONArray =
        JSONArray(
            fieldsMap.values
                .flatMap { field -> markerPattern.findAll(field).map { it.groupValues[1] } }
                .distinct()
        )

    fun consumeAnkiMessage() {
        _uiState.update {
            it.copy(
                ankiFieldsMessage = null
            )
        }
    }

}
