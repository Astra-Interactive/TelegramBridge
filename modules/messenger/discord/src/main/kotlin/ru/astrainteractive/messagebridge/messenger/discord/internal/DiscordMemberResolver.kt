package ru.astrainteractive.messagebridge.messenger.discord.internal

import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import net.dv8tion.jda.api.exceptions.ErrorResponseException
import net.dv8tion.jda.api.requests.ErrorResponse
import ru.astrainteractive.klibs.mikro.core.coroutines.propagateCancellationException
import ru.astrainteractive.klibs.mikro.core.logging.JUtiltLogger
import ru.astrainteractive.klibs.mikro.core.logging.Logger
import ru.astrainteractive.klibs.mikro.core.util.tryCast
import ru.astrainteractive.messagebridge.messaging.model.Text
import ru.astrainteractive.messagebridge.messenger.discord.api.DiscordAuthorResolver
import ru.astrainteractive.messagebridge.messenger.discord.util.await

internal class DiscordMemberResolver(
    private val authorResolver: DiscordAuthorResolver,
) : Logger by JUtiltLogger("MessageBridge-DiscordMemberResolver") {
    suspend fun resolve(channel: TextChannel, event: Text): Member? {
        val discordId = authorResolver.discordUserId(event) ?: return null
        return channel.guild.getMemberById(discordId)
            ?: runCatching { channel.guild.retrieveMemberById(discordId).await() }
                .propagateCancellationException()
                .onFailure { t ->
                    if (t.tryCast<ErrorResponseException>()?.errorResponse == ErrorResponse.UNKNOWN_MEMBER) {
                        info { "#resolve Discord user $discordId is not on the server: ${t.message}" }
                    } else {
                        warn { "#resolve could not fetch the member of Discord user $discordId: ${t.message}" }
                    }
                }
                .getOrNull()
    }
}
