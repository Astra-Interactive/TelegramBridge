package ru.astrainteractive.discordbot.module.bridge.model

import kotlinx.serialization.StringFormat
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.WebSocket

internal object SocketMessageFormat : StringFormat by Json(
    builderAction = {
        isLenient = true
        prettyPrint = true
        ignoreUnknownKeys = true
    }
)

internal inline fun <reified T> WebSocket.send(value: T): Boolean {
    return send(SocketMessageFormat.encodeToString(value))
}

internal inline fun <reified T> org.java_websocket.WebSocket.send(value: T) {
    return send(SocketMessageFormat.encodeToString(value))
}

internal inline fun <reified T> org.java_websocket.server.WebSocketServer.broadcast(value: T) {
    return broadcast(SocketMessageFormat.encodeToString(value))
}
