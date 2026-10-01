package ru.astrainteractive.messagebridge.link.discord.event

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import ru.astrainteractive.klibs.mikro.core.logging.JUtiltLogger
import ru.astrainteractive.klibs.mikro.core.logging.Logger
import ru.astrainteractive.messagebridge.core.api.config.PluginConfiguration
import ru.astrainteractive.messagebridge.link.api.api.DiscordMembership
import ru.astrainteractive.messagebridge.messenger.discord.api.util.findTextChannel

internal class DiscordMemberLeaveListener(
    private val configFlow: StateFlow<PluginConfiguration>,
    private val discordMembership: DiscordMembership,
    private val scope: CoroutineScope,
) : ListenerAdapter(),
    Logger by JUtiltLogger("MessageBridge-DiscordMemberLeaveListener") {

    override fun onGuildMemberRemove(event: GuildMemberRemoveEvent) {
        val bridgeGuild = event.jda.findTextChannel(configFlow.value.jdaConfig.channelId)?.guild
        if (bridgeGuild?.idLong != event.guild.idLong) {
            verbose { "#onGuildMemberRemove ${event.user.id} left ${event.guild.id}, not the bridge server" }
            return
        }
        scope.launch { discordMembership.revokeLeftMember(event.user.idLong) }
    }
}
