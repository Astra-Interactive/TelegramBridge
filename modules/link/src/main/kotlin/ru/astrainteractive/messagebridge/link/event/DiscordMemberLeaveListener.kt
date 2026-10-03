package ru.astrainteractive.messagebridge.link.event

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import ru.astrainteractive.messagebridge.link.api.LinkApi

internal class DiscordMemberLeaveListener(
    private val linkApi: LinkApi,
    private val ioScope: CoroutineScope
) : ListenerAdapter() {
    override fun onGuildMemberRemove(event: GuildMemberRemoveEvent) {
        ioScope.launch { linkApi.userLeaveDiscord(event.user) }
    }
}
