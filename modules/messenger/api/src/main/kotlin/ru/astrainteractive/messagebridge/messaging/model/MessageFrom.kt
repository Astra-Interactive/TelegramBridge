package ru.astrainteractive.messagebridge.messaging.model

import kotlinx.serialization.Serializable

@Serializable
enum class MessageFrom(val short: String) {
    MINECRAFT("MC"), TELEGRAM("TG"), DISCORD("DS")
}
