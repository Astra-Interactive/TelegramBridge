package ru.astrainteractive.messagebridge.messenger.discord.member

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import ru.astrainteractive.klibs.mikro.core.logging.JUtiltLogger
import ru.astrainteractive.klibs.mikro.core.logging.Logger
import ru.astrainteractive.messagebridge.link.DiscordMembership
import ru.astrainteractive.messagebridge.messenger.discord.channel.DiscordChannelProvider

/**
 * Takes back what linking gave when a member leaves the server of the bridge channel. Discord sends the event
 * only with Server Members Intent, which the bot asks for while linking gives roles.
 */
internal class DiscordMemberLeaveListener(
    private val channelProvider: DiscordChannelProvider,
    private val discordMembership: DiscordMembership,
    private val scope: CoroutineScope,
) : ListenerAdapter(),
    Logger by JUtiltLogger("MessageBridge-DiscordMemberLeaveListener") {

    override fun onGuildMemberRemove(event: GuildMemberRemoveEvent) {
        val bridgeGuild = channelProvider.textChannel(event.jda).getOrNull()?.guild
        // Players link on the server of the bridge channel, so leaving another server of the bot changes nothing
        if (bridgeGuild?.idLong != event.guild.idLong) {
            verbose { "#onGuildMemberRemove ${event.user.id} left ${event.guild.id}, not the bridge server" }
            return
        }
        scope.launch { discordMembership.revokeLeftMember(event.user.idLong) }
    }
}
