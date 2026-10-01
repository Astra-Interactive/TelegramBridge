package ru.astrainteractive.messagebridge.messenger.discord.relay.event

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import ru.astrainteractive.messagebridge.messenger.api.impl.BEventChannel
import ru.astrainteractive.messagebridge.messenger.api.model.Text
import ru.astrainteractive.messagebridge.messenger.discord.command.internal.DiscordCommandHandler
import ru.astrainteractive.messagebridge.messenger.discord.command.internal.DiscordCommandMapper
import ru.astrainteractive.messagebridge.messenger.discord.onboarding.internal.DiscordBindHandler
import ru.astrainteractive.messagebridge.messenger.discord.relay.internal.DiscordMessageRelevanceMapper
import ru.astrainteractive.messagebridge.messenger.discord.relay.internal.DiscordReplyMapper
import ru.astrainteractive.messagebridge.messenger.discord.relay.model.DiscordMessageRelevance

internal class DiscordMessageListener(
    private val relevanceMapper: DiscordMessageRelevanceMapper,
    private val commandMapper: DiscordCommandMapper,
    private val commandHandler: DiscordCommandHandler,
    private val replyMapper: DiscordReplyMapper,
    private val bindHandler: DiscordBindHandler,
    private val scope: CoroutineScope,
) : ListenerAdapter() {

    private suspend fun relay(event: MessageReceivedEvent) {
        BEventChannel.consume(
            Text.Discord(
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
        val bindCode = commandMapper.mapBindCode(event.message.contentRaw)
        if (bindCode != null && event.isFromGuild && !event.isWebhookMessage && !event.author.isBot) {
            scope.launch { bindHandler.bind(bindCode, event) }
            return
        }
        when (relevanceMapper.map(event)) {
            DiscordMessageRelevance.Relevant -> scope.launch { process(event) }
            DiscordMessageRelevance.PrivateMessage -> scope.launch { commandHandler.linkFromPrivate(event) }
            DiscordMessageRelevance.WebhookMessage,
            DiscordMessageRelevance.BotAuthor,
            DiscordMessageRelevance.WrongChannel -> Unit
        }
    }
}
