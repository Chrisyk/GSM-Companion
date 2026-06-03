package com.example.gsmcompanion

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class TextHookerUiState(
    val connectionStatus : ConnectionStatus =
        ConnectionStatus.Disconnected,
    val sentences: List<String> = emptyList(),
    val errorMessage: String? = null
)

class TextHookerViewModel : ViewModel(){
    private val client = TextHookerClient()
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
        client.close()

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

}
