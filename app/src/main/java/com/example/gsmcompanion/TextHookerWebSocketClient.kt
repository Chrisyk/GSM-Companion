package com.example.gsmcompanion
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString

class TextHookerWebSocketClient : WebSocketListener() {

    override fun onOpen(webSocket: WebSocket, response: Response) {
        println("Connection opened")
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
        println("Receiving Text: $text")
    }

    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
        println("Closing: $code / $reason")
    }

    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
        println("Closing: $code / $reason")
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        println("Error: ${t.message}")
    }

    companion object {
        const val NORMAL_CLOSURE_STATUS = 1000
    }
}