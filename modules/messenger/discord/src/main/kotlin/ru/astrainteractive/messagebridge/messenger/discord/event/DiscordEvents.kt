package ru.astrainteractive.messagebridge.messenger.discord.event

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.IEventManager
import ru.astrainteractive.messagebridge.messenger.discord.api.DiscordMemberLeaveListener
import ru.astrainteractive.messagebridge.messenger.discord.internal.flowEvent

internal class DiscordEvents(
    eventManager: IEventManager,
    messageEventListener: MessageEventListener,
    memberLeaveListeners: List<DiscordMemberLeaveListener>,
    ioScope: CoroutineScope
) {
    val messageReceivedEvent = eventManager.flowEvent<MessageReceivedEvent>()
        .onEach(messageEventListener::onMessageReceived)
        .launchIn(ioScope)

    val guildMemberRemoveEvent = eventManager.flowEvent<GuildMemberRemoveEvent>()
        .map { event -> event.user }
        .map { user -> user.idLong }
        .onEach { discordUserId ->
            ioScope.launch {
                memberLeaveListeners.forEach { listener -> listener.onMemberLeave(discordUserId) }
            }
        }
        .launchIn(ioScope)
}
