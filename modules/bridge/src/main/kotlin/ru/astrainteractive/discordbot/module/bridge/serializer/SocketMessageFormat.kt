package ru.astrainteractive.discordbot.module.bridge.serializer

import kotlinx.serialization.StringFormat
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.WebSocket

internal object SocketMessageFormat : StringFormat by Json(
    builderAction = {
        isLenient = true
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
)

internal suspend inline fun <reified T> WebSocket.send(value: T): Boolean {
    return send(SocketMessageFormat.encodeToString(value))
}

internal suspend inline fun <reified T> org.java_websocket.WebSocket.send(value: T) {
    return send(SocketMessageFormat.encodeToString(value))
}

internal suspend inline fun <reified T> org.java_websocket.server.WebSocketServer.broadcast(value: T) {
    return broadcast(SocketMessageFormat.encodeToString(value))
}
