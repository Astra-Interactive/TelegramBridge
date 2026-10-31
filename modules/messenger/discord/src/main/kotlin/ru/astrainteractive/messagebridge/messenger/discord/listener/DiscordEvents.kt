@file:Suppress("KindImportDirection", "TrivialLambdaReference")

package ru.astrainteractive.messagebridge.messenger.discord.listener

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.IEventManager
import ru.astrainteractive.messagebridge.messenger.api.discord.model.DiscordMemberLeave
import ru.astrainteractive.messagebridge.messenger.api.discord.model.DiscordUserId
import ru.astrainteractive.messagebridge.messenger.discord.message.MessageEventListener
import ru.astrainteractive.messagebridge.messenger.discord.util.flowEvent

internal class DiscordEvents(
    eventManager: IEventManager,
    messageEventListener: MessageEventListener,
    memberLeaves: SendChannel<DiscordMemberLeave>,
    ioScope: CoroutineScope
) {
    val messageReceivedEvent = eventManager.flowEvent<MessageReceivedEvent>()
        .onEach(messageEventListener::onMessageReceived)
        .launchIn(ioScope)

    val guildMemberRemoveEvent = eventManager.flowEvent<GuildMemberRemoveEvent>()
        .map { event -> event.user }
        .map { user -> user.idLong }
        .onEach { discordUserId -> memberLeaves.send(DiscordMemberLeave(DiscordUserId(discordUserId))) }
        .launchIn(ioScope)
}
