package com.example.gsmcompanion

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class ConnectionStatus {
    Disconnected,
    Connecting,
    Connected,
    Error,
}

data class TextHookerUiState(
    val connectionStatus : ConnectionStatus =
        ConnectionStatus.Disconnected,
    val sentences: List<String> = emptyList(),
    val errorMessage: String? = null
)

class TextHookerViewModel : ViewModel(){
    private val client = TextHookerClient()
    private val _uiState =
        MutableStateFlow(TextHookerUiState())
    val uiState = _uiState.asStateFlow()
    private var currentUrl: String? = null

    fun connect(url: String) {
        if (currentUrl == url &&
            (_uiState.value.connectionStatus == ConnectionStatus.Connecting ||
                _uiState.value.connectionStatus == ConnectionStatus.Connected)
        ) return
        client.close()
        currentUrl = url

        _uiState.update{
            it.copy(
                connectionStatus=
                    ConnectionStatus.Connecting,
                errorMessage = null
            )
        }

        client.connect(
            url = url,
            onOpen = {
                _uiState.update {
                    it.copy(connectionStatus=
                        ConnectionStatus.Connected)

                }
            },
            onMessage = { message ->
                _uiState.update { state ->
                    state.copy(
                        sentences=
                            (state.sentences + message).takeLast(300)
                    )
                }
            },
            onClosed = {
                _uiState.update {
                    it.copy(connectionStatus=
                    ConnectionStatus.Disconnected)
                }
            },
            onFailure = { error ->
                _uiState.update {
                    it.copy(connectionStatus=
                    ConnectionStatus.Error,
                        errorMessage=
                    error.message
                    )
                }
            }
        )
    }

    override fun onCleared(){
        client.close()
    }

    fun statusMessage() : String {
        return when (uiState.value.connectionStatus) {
            ConnectionStatus.Connected -> "Connected"
            ConnectionStatus.Connecting -> "Connecting"
            ConnectionStatus.Disconnected -> "Disconnected"
            ConnectionStatus.Error -> _uiState.value.errorMessage ?: "Error"
        }
    }

}
