package ru.astrainteractive.telegrambridge.messaging


sealed interface Message {
    enum class MessageFrom {
        MINECRAFT, TELEGRAM, DISCORD
    }

    val from: MessageFrom

    data class Text(val _text: String, override val from: MessageFrom) : Message {
        val text: String
            get() = "[$from] $_text"
    }
}
