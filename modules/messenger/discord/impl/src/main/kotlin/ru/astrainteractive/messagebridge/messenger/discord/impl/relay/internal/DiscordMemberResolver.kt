package ru.astrainteractive.messagebridge.messenger.discord.impl.relay.internal

import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import net.dv8tion.jda.api.exceptions.ErrorResponseException
import net.dv8tion.jda.api.requests.ErrorResponse
import ru.astrainteractive.messagebridge.link.api.player.api.LinkingDao
import ru.astrainteractive.messagebridge.messenger.api.model.BEvent
import ru.astrainteractive.messagebridge.messenger.discord.api.util.awaitRequest
import java.util.UUID

internal class DiscordMemberResolver(
    private val linkingDao: LinkingDao,
) {
    private suspend fun linkedDiscordId(event: BEvent.Text): Long? {
        val linkedPlayer = when (event) {
            is BEvent.Text.Discord -> linkingDao.findByDiscordId(event.authorId).getOrNull()
            is BEvent.Text.Minecraft -> linkingDao.findByUuid(UUID.fromString(event.uuid)).getOrNull()
            is BEvent.Text.Telegram -> linkingDao.findByTelegramId(event.authorId).getOrNull()
        }
        return linkedPlayer?.discordLink?.discordId
    }

    private fun isNotOnServer(t: Throwable): Boolean {
        return t is ErrorResponseException && t.errorResponse in MEMBER_GONE
    }

    suspend fun resolve(channel: TextChannel, event: BEvent.Text): Result<Member?> {
        val discordId = linkedDiscordId(event) ?: return Result.success(null)
        val cachedMember = channel.guild.getMemberById(discordId)
        if (cachedMember != null) return Result.success(cachedMember)
        val member = awaitRequest { channel.guild.retrieveMemberById(discordId) }.getOrElse { t ->
            return if (isNotOnServer(t)) Result.success(null) else Result.failure(t)
        }
        return Result.success(member)
    }

    private companion object {
        val MEMBER_GONE = setOf(ErrorResponse.UNKNOWN_MEMBER, ErrorResponse.UNKNOWN_USER)
    }
}
