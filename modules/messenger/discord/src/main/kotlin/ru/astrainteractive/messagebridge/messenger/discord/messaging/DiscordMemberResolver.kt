package ru.astrainteractive.messagebridge.messenger.discord.messaging

import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import ru.astrainteractive.klibs.mikro.core.coroutines.propagateCancellationException
import ru.astrainteractive.klibs.mikro.core.logging.JUtiltLogger
import ru.astrainteractive.klibs.mikro.core.logging.Logger
import ru.astrainteractive.messagebridge.messaging.model.Text
import ru.astrainteractive.messagebridge.messenger.discord.api.DiscordAuthorResolver
import ru.astrainteractive.messagebridge.messenger.discord.model.DiscordRequestCancelledError
import ru.astrainteractive.messagebridge.messenger.discord.util.RestActionExt.await

internal class DiscordMemberResolver(
    private val authorResolver: DiscordAuthorResolver,
) : Logger by JUtiltLogger("MessageBridge-DiscordMemberResolver") {
    suspend fun resolve(channel: TextChannel, event: Text): Member? {
        val discordId = authorResolver.discordUserId(event) ?: return null
        return channel.guild.getMemberById(discordId)
            ?: runCatching { channel.guild.retrieveMemberById(discordId).await() }
                .propagateCancellationException()
                .onFailure { t ->
                    if (t is DiscordRequestCancelledError) {
                        warn { "#resolve JDA cancelled the member request of Discord user $discordId" }
                    } else {
                        info { "#resolve Discord user $discordId is not on the server: ${t.message}" }
                    }
                }
                .getOrNull()
    }
}
