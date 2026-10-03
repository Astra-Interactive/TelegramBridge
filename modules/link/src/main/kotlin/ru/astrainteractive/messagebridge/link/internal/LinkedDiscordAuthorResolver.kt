package ru.astrainteractive.messagebridge.link.internal

import ru.astrainteractive.messagebridge.link.database.dao.LinkingDao
import ru.astrainteractive.messagebridge.messaging.model.Text
import ru.astrainteractive.messagebridge.messenger.discord.api.DiscordAuthorResolver
import java.util.UUID

internal class LinkedDiscordAuthorResolver(
    private val linkingDao: LinkingDao
) : DiscordAuthorResolver {
    override suspend fun discordUserId(text: Text): Long? {
        val linkedPlayer = when (text) {
            is Text.Discord -> linkingDao.findByDiscordId(text.authorId).getOrNull()
            is Text.Minecraft -> linkingDao.findByUuid(UUID.fromString(text.uuid)).getOrNull()
            is Text.Telegram -> linkingDao.findByTelegramId(text.authorId).getOrNull()
        }
        return linkedPlayer?.discordLink?.discordId
    }
}
