package ru.astrainteractive.messagebridge.messenger.discord.impl.relay.event

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import ru.astrainteractive.messagebridge.messenger.api.impl.BEventChannel
import ru.astrainteractive.messagebridge.messenger.api.model.BEvent
import ru.astrainteractive.messagebridge.messenger.discord.api.api.DiscordMessageInterceptor
import ru.astrainteractive.messagebridge.messenger.discord.impl.command.internal.DiscordCommandHandler
import ru.astrainteractive.messagebridge.messenger.discord.impl.command.internal.DiscordCommandMapper
import ru.astrainteractive.messagebridge.messenger.discord.impl.relay.internal.DiscordMessageRelevanceMapper
import ru.astrainteractive.messagebridge.messenger.discord.impl.relay.internal.DiscordReplyMapper
import ru.astrainteractive.messagebridge.messenger.discord.impl.relay.model.DiscordMessageRelevance

internal class DiscordMessageListener(
    private val relevanceMapper: DiscordMessageRelevanceMapper,
    private val commandMapper: DiscordCommandMapper,
    private val commandHandler: DiscordCommandHandler,
    private val replyMapper: DiscordReplyMapper,
    private val messageInterceptors: () -> List<DiscordMessageInterceptor>,
    private val scope: CoroutineScope,
    private val bEventChannel: BEventChannel,
) : ListenerAdapter() {

    private suspend fun relay(event: MessageReceivedEvent) {
        bEventChannel.consume(
            BEvent.Text.Discord(
                author = event.member?.nickname ?: event.author.name,
                text = event.message.contentRaw,
                authorId = event.author.idLong,
                reply = replyMapper.map(event.message),
            )
        )
    }

    private suspend fun process(event: MessageReceivedEvent) {
        val command = commandMapper.map(event.message.contentRaw)
        if (command != null) {
            commandHandler.handle(command, event)
            return
        }
        relay(event)
    }

    override fun onMessageReceived(event: MessageReceivedEvent) {
        if (messageInterceptors.invoke().any { interceptor -> interceptor.intercept(event) }) return
        when (relevanceMapper.map(event)) {
            DiscordMessageRelevance.Relevant -> scope.launch { process(event) }
            DiscordMessageRelevance.WebhookMessage,
            DiscordMessageRelevance.BotAuthor,
            DiscordMessageRelevance.WrongChannel -> Unit
        }
    }
}
