package ru.astrainteractive.messagebridge.messenger.discord.member

import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.requests.GatewayIntent
import ru.astrainteractive.klibs.mikro.core.logging.JUtiltLogger
import ru.astrainteractive.klibs.mikro.core.logging.Logger
import ru.astrainteractive.messagebridge.link.DiscordMembership
import ru.astrainteractive.messagebridge.messenger.discord.channel.DiscordChannelProvider
import ru.astrainteractive.messagebridge.messenger.discord.connection.awaitTask

/**
 * Catches up on the members who left the server of the bridge channel while the bot was offline, because Discord
 * sends no leave events for that time.
 */
internal class DiscordMemberSweep(
    private val channelProvider: DiscordChannelProvider,
    private val discordMembership: DiscordMembership,
) : Logger by JUtiltLogger("MessageBridge-DiscordMemberSweep") {

    suspend fun sweep(jda: JDA) {
        // The bot asks for the members only while linking gives roles, and cannot load them without it
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
