package ru.astrainteractive.messagebridge.link.internal

import ru.astrainteractive.klibs.mikro.core.logging.JUtiltLogger
import ru.astrainteractive.klibs.mikro.core.logging.Logger
import ru.astrainteractive.messagebridge.link.dao.api.LinkingDao
import ru.astrainteractive.messagebridge.messaging.model.Text
import ru.astrainteractive.messagebridge.messenger.discord.api.DiscordAuthorResolver
import java.util.UUID

internal class LinkedDiscordAuthorResolver(
    private val linkingDao: LinkingDao
) : DiscordAuthorResolver, Logger by JUtiltLogger("MessageBridge-LinkedDiscordAuthorResolver") {
    override suspend fun discordUserId(text: Text): Long? {
        val linkedPlayer = when (text) {
            is Text.Discord -> linkingDao.findByDiscordId(text.authorId)
            is Text.Minecraft -> linkingDao.findByUuid(UUID.fromString(text.uuid))
            is Text.Telegram -> linkingDao.findByTelegramId(text.authorId)
        }
        return linkedPlayer
            .onFailure { t -> error(t) { "#discordUserId could not read the link of ${text.author}" } }
            .getOrNull()
            ?.discord
            ?.id
    }
}
