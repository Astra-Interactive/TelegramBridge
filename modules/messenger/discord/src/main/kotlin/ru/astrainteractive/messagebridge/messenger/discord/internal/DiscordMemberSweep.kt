package ru.astrainteractive.messagebridge.messenger.discord.internal

import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.requests.GatewayIntent
import ru.astrainteractive.klibs.mikro.core.logging.JUtiltLogger
import ru.astrainteractive.klibs.mikro.core.logging.Logger
import ru.astrainteractive.messagebridge.link.api.DiscordMembership
import ru.astrainteractive.messagebridge.messenger.discord.channel.internal.DiscordChannelProvider
import ru.astrainteractive.messagebridge.messenger.discord.util.awaitTask

internal class DiscordMemberSweep(
    private val channelProvider: DiscordChannelProvider,
    private val discordMembership: DiscordMembership,
) : Logger by JUtiltLogger("MessageBridge-DiscordMemberSweep") {

    suspend fun sweep(jda: JDA) {
        if (GatewayIntent.GUILD_MEMBERS !in jda.gatewayIntents) return
        val guild = channelProvider.textChannel(jda).getOrElse { failure ->
            verbose { "#sweep the bridge channel is not available: $failure" }
            return
        }.guild
        val members = awaitTask { guild.loadMembers() }.getOrElse { failure ->
            error(failure) { "#sweep could not load the members of ${guild.name}" }
            return
        }
        verbose { "#sweep ${members.size} members are on ${guild.name}" }
        discordMembership.revokeAbsentMembers(members.mapTo(HashSet()) { member -> member.idLong })
    }
}
