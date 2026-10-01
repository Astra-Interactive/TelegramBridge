package ru.astrainteractive.messagebridge.link.impl.player.api

import ru.astrainteractive.messagebridge.link.api.player.model.LinkedPlayerModel

internal interface DiscordLinkedPlayerDao {
    suspend fun findAllWithDiscordLink(): Result<List<LinkedPlayerModel>>
}
