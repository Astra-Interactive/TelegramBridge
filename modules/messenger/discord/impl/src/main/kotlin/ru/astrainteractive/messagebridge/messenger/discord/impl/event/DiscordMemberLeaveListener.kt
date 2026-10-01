package ru.astrainteractive.messagebridge.messenger.discord.impl.event

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import ru.astrainteractive.klibs.mikro.core.logging.JUtiltLogger
import ru.astrainteractive.klibs.mikro.core.logging.Logger
import ru.astrainteractive.messagebridge.link.api.DiscordMembership
import ru.astrainteractive.messagebridge.messenger.discord.impl.channel.internal.DiscordChannelProvider

internal class DiscordMemberLeaveListener(
    private val channelProvider: DiscordChannelProvider,
    private val discordMembership: DiscordMembership,
    private val scope: CoroutineScope,
) : ListenerAdapter(),
    Logger by JUtiltLogger("MessageBridge-DiscordMemberLeaveListener") {

    override fun onGuildMemberRemove(event: GuildMemberRemoveEvent) {
        val bridgeGuild = channelProvider.textChannel(event.jda).getOrNull()?.guild
        if (bridgeGuild?.idLong != event.guild.idLong) {
            verbose { "#onGuildMemberRemove ${event.user.id} left ${event.guild.id}, not the bridge server" }
            return
        }
        scope.launch { discordMembership.revokeLeftMember(event.user.idLong) }
    }
}
