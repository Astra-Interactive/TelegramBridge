package ru.astrainteractive.messagebridge.messenger.discord.messaging

import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import ru.astrainteractive.messagebridge.link.database.dao.LinkingDao
import ru.astrainteractive.messagebridge.messaging.model.Text
import ru.astrainteractive.messagebridge.messenger.discord.util.RestActionExt.await
import java.util.UUID

internal class DiscordMemberResolver(
    private val linkingDao: LinkingDao,
) {
    suspend fun resolve(channel: TextChannel, event: Text): Member? {
        val discordId = linkedDiscordId(event) ?: return null
        return channel.guild.getMemberById(discordId)
            ?: channel.guild.retrieveMemberById(discordId).await()
    }

    private suspend fun linkedDiscordId(event: Text): Long? {
        val linkedPlayer = when (event) {
            is Text.Discord -> linkingDao.findByDiscordId(event.authorId).getOrNull()
            is Text.Minecraft -> linkingDao.findByUuid(UUID.fromString(event.uuid)).getOrNull()
            is Text.Telegram -> linkingDao.findByTelegramId(event.authorId).getOrNull()
        }
        return linkedPlayer?.discordLink?.discordId
    }
}
