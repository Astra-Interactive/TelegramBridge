package ru.astrainteractive.messagebridge.link.dao.model

internal sealed interface MessengerAccount {
    val id: Long

    data class Discord(override val id: Long, val name: String) : MessengerAccount

    data class Telegram(override val id: Long, val username: String) : MessengerAccount
}
