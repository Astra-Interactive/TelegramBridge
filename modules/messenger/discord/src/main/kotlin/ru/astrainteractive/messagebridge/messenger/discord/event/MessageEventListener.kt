package ru.astrainteractive.messagebridge.messenger.discord.event

import kotlinx.coroutines.launch
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import ru.astrainteractive.astralibs.coroutines.withTimings
import ru.astrainteractive.klibs.mikro.core.coroutines.CoroutineFeature
import ru.astrainteractive.klibs.mikro.core.logging.JUtiltLogger
import ru.astrainteractive.klibs.mikro.core.logging.Logger
import ru.astrainteractive.messagebridge.link.api.LinkApi
import ru.astrainteractive.messagebridge.messaging.internal.BEventChannel
import ru.astrainteractive.messagebridge.messaging.model.Text
import ru.astrainteractive.messagebridge.messenger.discord.event.core.DiscordEventListener
import ru.astrainteractive.messagebridge.messenger.discord.mapping.DiscordCommandMapper
import ru.astrainteractive.messagebridge.messenger.discord.mapping.DiscordMessageRelevanceMapper
import ru.astrainteractive.messagebridge.messenger.discord.model.DiscordMessageRelevance

internal class MessageEventListener(
    private val relevanceMapper: DiscordMessageRelevanceMapper,
    private val commandMapper: DiscordCommandMapper,
    private val commandHandler: DiscordCommandHandler,
    private val linkApi: LinkApi,
) : ListenerAdapter(),
    DiscordEventListener,
    CoroutineFeature by CoroutineFeature.IO.withTimings(),
    Logger by JUtiltLogger("MessageEventListener") {

    override fun onGuildMemberRemove(event: GuildMemberRemoveEvent) {
        super.onGuildMemberRemove(event)
        launch { linkApi.userLeaveDiscord(event.user) }
    }

    override fun onMessageReceived(event: MessageReceivedEvent) {
        when (relevanceMapper.map(event)) {
            DiscordMessageRelevance.Relevant -> launch { process(event) }
            DiscordMessageRelevance.PrivateMessage -> launch { commandHandler.linkFromPrivate(event) }
            DiscordMessageRelevance.WebhookMessage,
            DiscordMessageRelevance.BotAuthor,
            DiscordMessageRelevance.WrongChannel -> Unit
        }
    }

    private suspend fun process(event: MessageReceivedEvent) {
        val command = commandMapper.map(event.message.contentRaw)
        if (command != null) {
            commandHandler.handle(command, event)
            return
        }
        relay(event)
    }

    private suspend fun relay(event: MessageReceivedEvent) {
        BEventChannel.consume(
            Text.Discord(
                author = event.member?.nickname ?: event.author.name,
                text = event.message.contentRaw,
                authorId = event.author.idLong,
            )
        )
    }
}
