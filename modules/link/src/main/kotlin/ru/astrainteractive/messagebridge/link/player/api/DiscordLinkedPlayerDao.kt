package ru.astrainteractive.messagebridge.link.player.api

import ru.astrainteractive.messagebridge.link.player.model.LinkedPlayerModel

internal interface DiscordLinkedPlayerDao {
    suspend fun findAllWithDiscordLink(): Result<List<LinkedPlayerModel>>
}
