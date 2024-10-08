package ru.astrainteractive.messagebridge.messaging.model

sealed interface Message {
    enum class MessageFrom {
        MINECRAFT, TELEGRAM, DISCORD
    }

    val from: MessageFrom

    data class Text(
        val author: String,
        val text: String,
        override val from: MessageFrom
    ) : Message

    data class PlayerJoined(val name: String) : Message {
        override val from: MessageFrom = MessageFrom.MINECRAFT
    }

    data class PlayerLeave(val name: String) : Message {
        override val from: MessageFrom = MessageFrom.MINECRAFT
    }

    data class PlayerDeath(val name: String, val cause: String? = null) : Message {
        override val from: MessageFrom = MessageFrom.MINECRAFT
    }
}
