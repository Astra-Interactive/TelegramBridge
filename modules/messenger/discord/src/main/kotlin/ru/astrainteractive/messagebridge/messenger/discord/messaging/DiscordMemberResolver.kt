package ru.astrainteractive.messagebridge.messenger.discord.messaging

import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import ru.astrainteractive.messagebridge.messaging.model.Text
import ru.astrainteractive.messagebridge.messenger.discord.api.DiscordAuthorResolver
import ru.astrainteractive.messagebridge.messenger.discord.util.RestActionExt.await

internal class DiscordMemberResolver(
    private val authorResolver: DiscordAuthorResolver,
) {
    suspend fun resolve(channel: TextChannel, event: Text): Member? {
        val discordId = authorResolver.discordUserId(event) ?: return null
        return channel.guild.getMemberById(discordId)
            ?: channel.guild.retrieveMemberById(discordId).await()
    }
}
