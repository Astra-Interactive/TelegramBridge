package ru.astrainteractive.messagebridge.messenger.discord.relay

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import ru.astrainteractive.messagebridge.messaging.internal.BEventChannel
import ru.astrainteractive.messagebridge.messaging.model.Text
import ru.astrainteractive.messagebridge.messenger.discord.command.DiscordCommandHandler
import ru.astrainteractive.messagebridge.messenger.discord.command.DiscordCommandMapper
import ru.astrainteractive.messagebridge.messenger.discord.onboarding.DiscordBindHandler

/** Sends every Discord message where it belongs: a bind code, a bot command, a linking code or the chat. */
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
        // A channel is bound before it is the bridge channel, so the code is looked for in every channel
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
