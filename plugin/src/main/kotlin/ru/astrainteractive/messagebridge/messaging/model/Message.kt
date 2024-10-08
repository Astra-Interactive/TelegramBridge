package ru.astrainteractive.messagebridge.messaging.model

sealed interface Message {
    enum class MessageFrom {
        MINECRAFT, TELEGRAM, DISCORD
    }

    val from: MessageFrom

    data class Text(
        val textInternal: String,
        override val from: MessageFrom
    ) : Message {
        val formattedText: String = "[$from] $textInternal"
    }
}
