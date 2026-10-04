package ru.astrainteractive.messagebridge.link.player.model

internal sealed interface MessengerAccount {
    val id: Long

    data class Discord(override val id: Long, val name: String) : MessengerAccount

    data class Telegram(override val id: Long, val username: String) : MessengerAccount
}
