package ru.astrainteractive.messagebridge.link.player.model

import java.util.UUID

internal data class LinkedPlayer(
    val uuid: UUID,
    val minecraftName: String,
    val discord: MessengerAccount.Discord?,
    val telegram: MessengerAccount.Telegram?
)
