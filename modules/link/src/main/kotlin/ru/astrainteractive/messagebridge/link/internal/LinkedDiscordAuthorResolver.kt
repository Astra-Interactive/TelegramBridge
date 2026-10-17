package ru.astrainteractive.messagebridge.link.internal

import ru.astrainteractive.klibs.mikro.core.logging.JUtiltLogger
import ru.astrainteractive.klibs.mikro.core.logging.Logger
import ru.astrainteractive.messagebridge.link.api.LinkedPlayerRepository
import ru.astrainteractive.messagebridge.messenger.api.model.Text
import ru.astrainteractive.messagebridge.messenger.discord.api.DiscordAuthorResolver
import java.util.UUID

internal class LinkedDiscordAuthorResolver(
    private val linkedPlayerRepository: LinkedPlayerRepository
) : DiscordAuthorResolver, Logger by JUtiltLogger("MessageBridge-LinkedDiscordAuthorResolver") {
    override suspend fun discordUserId(text: Text): Long? {
        val linkedPlayer = when (text) {
            is Text.Discord -> linkedPlayerRepository.findByDiscordId(text.authorId)
            is Text.Minecraft -> linkedPlayerRepository.findByUuid(UUID.fromString(text.uuid))
            is Text.Telegram -> linkedPlayerRepository.findByTelegramId(text.authorId)
        }
        return linkedPlayer
            .onFailure { t -> error(t) { "#discordUserId could not read the link of ${text.author}" } }
            .getOrNull()
            ?.discord
            ?.id
    }
}
