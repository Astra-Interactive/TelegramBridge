package ru.astrainteractive.messagebridge.messenger.api.model

import kotlinx.serialization.Serializable

@Serializable
enum class MessageFrom(val short: String) {
    MINECRAFT("MC"), TELEGRAM("TG"), DISCORD("DS")
}
