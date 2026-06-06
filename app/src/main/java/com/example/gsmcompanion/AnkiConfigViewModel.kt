package com.example.gsmcompanion

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class AnkiConfigViewModel(application : Application) : AndroidViewModel(application) {
    private val client = okhttp3.OkHttpClient()

    val configs = APIConfs.getConfigs(application).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = Configs()
    )

    val ankiConnectUrl = APIConfs.getURL(configs.value, PortName.AnkiConnect)

    suspend fun getModelNames() : List<String> {

        val body = JSONObject().apply {
            put("action", "modelNames")
            put("version", 5)
        }.toString()
        val request = Request.Builder()
            .url(ankiConnectUrl)
            .post(body.toRequestBody())
            .build()

        return withContext(Dispatchers.IO){
            client.newCall(request).execute().use { response ->
                val body = response.body.string()
                val result = JSONObject(body).getJSONArray("result")
                List(result.length()) { i -> result.getString(i) }
            }
        }
    }

    suspend fun getModelFieldNames(modelName : String) : List<String> {
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

        return withContext(Dispatchers.IO){
            client.newCall(request).execute().use { response ->
                val body = response.body.string()
                val result = JSONObject(body).getJSONArray("result")
                List(result.length()) { i -> result.getString(i)}
            }
        }
    }
}