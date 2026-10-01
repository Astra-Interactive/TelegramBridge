package ru.astrainteractive.messagebridge.messenger.discord.relay

import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import net.dv8tion.jda.api.exceptions.ErrorResponseException
import net.dv8tion.jda.api.requests.ErrorResponse
import ru.astrainteractive.messagebridge.link.player.LinkingDao
import ru.astrainteractive.messagebridge.messaging.model.Text
import ru.astrainteractive.messagebridge.messenger.discord.connection.awaitRequest
import java.util.UUID

/** The Discord member linked to the author of a message, whose name and avatar the webhook then shows. */
internal class DiscordMemberResolver(
    private val linkingDao: LinkingDao,
) {
    private suspend fun linkedDiscordId(event: Text): Long? {
        val linkedPlayer = when (event) {
            is Text.Discord -> linkingDao.findByDiscordId(event.authorId).getOrNull()
            is Text.Minecraft -> linkingDao.findByUuid(UUID.fromString(event.uuid)).getOrNull()
            is Text.Telegram -> linkingDao.findByTelegramId(event.authorId).getOrNull()
        }
        return linkedPlayer?.discordLink?.discordId
    }

    private fun isNotOnServer(failure: Throwable): Boolean {
        return failure is ErrorResponseException && failure.errorResponse in MEMBER_GONE
    }

    /**
     * @return `null` when the author has no linked Discord account or the linked member left the server, so the
     * message is sent under the name of the author
     */
    suspend fun resolve(channel: TextChannel, event: Text): Result<Member?> {
        val discordId = linkedDiscordId(event) ?: return Result.success(null)
        val cachedMember = channel.guild.getMemberById(discordId)
        if (cachedMember != null) return Result.success(cachedMember)
        val member = awaitRequest { channel.guild.retrieveMemberById(discordId) }.getOrElse { failure ->
            return if (isNotOnServer(failure)) Result.success(null) else Result.failure(failure)
        }
        return Result.success(member)
    }

    private companion object {
        val MEMBER_GONE = setOf(ErrorResponse.UNKNOWN_MEMBER, ErrorResponse.UNKNOWN_USER)
    }
}
