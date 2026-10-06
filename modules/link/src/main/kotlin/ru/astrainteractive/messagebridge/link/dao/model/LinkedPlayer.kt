package ru.astrainteractive.messagebridge.link.dao.model

import java.util.UUID

internal data class LinkedPlayer(
    val uuid: UUID,
    val minecraftName: String,
    val discord: MessengerAccount.Discord?,
    val telegram: MessengerAccount.Telegram?
)
