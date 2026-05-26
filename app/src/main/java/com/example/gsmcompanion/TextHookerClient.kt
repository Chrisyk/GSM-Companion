package com.example.gsmcompanion
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit

class TextHookerClient(
    private val okHttpClient: OkHttpClient =
        OkHttpClient.Builder()
            .readTimeout(0, TimeUnit.MILLISECONDS)
            .build()
) {

    private var webSocket : WebSocket? = null

    fun connect(
        url: String,
        onOpen: () -> Unit,
        onMessage: (String) -> Unit,
        onClosed: () -> Unit,
        onFailure: (Throwable) -> Unit,
    ) {
        if (webSocket != null) return

        val request = Request.Builder()
            .url(url)
            .build()
        webSocket = okHttpClient.newWebSocket(
            request,
            object: WebSocketListener() {
                override fun onOpen(
                    webSocket:
                    WebSocket, response: Response
                ) {
                    println("WEBSOCKET onOpen")
                    onOpen()
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    onMessage(text)
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    println("WEBSOCKET onClosed code=$code reason=$reason")
                    this@TextHookerClient.webSocket = null
                    onClosed()
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    println("WEBSOCKET onFailure ${t.message}")
                    this@TextHookerClient.webSocket = null
                    onFailure(t)
                }
            }
        )
    }

    fun close() {
        webSocket?.close(1000, "Closing texthooker websocket")
        webSocket = null
    }
}
