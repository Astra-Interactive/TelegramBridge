package ru.astrainteractive.messagebridge.messenger.discord.event

import kotlinx.coroutines.launch
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import ru.astrainteractive.astralibs.coroutines.withTimings
import ru.astrainteractive.klibs.mikro.core.coroutines.CoroutineFeature
import ru.astrainteractive.klibs.mikro.core.logging.JUtiltLogger
import ru.astrainteractive.klibs.mikro.core.logging.Logger
import ru.astrainteractive.messagebridge.messaging.api.BEventConsumer
import ru.astrainteractive.messagebridge.messaging.api.MessageInterceptor
import ru.astrainteractive.messagebridge.messaging.api.intercept
import ru.astrainteractive.messagebridge.messaging.model.Interception
import ru.astrainteractive.messagebridge.messaging.model.Text
import ru.astrainteractive.messagebridge.messenger.discord.mapping.DiscordCommandMapper
import ru.astrainteractive.messagebridge.messenger.discord.mapping.DiscordMessageRelevanceMapper
import ru.astrainteractive.messagebridge.messenger.discord.mapping.DiscordReplyMapper
import ru.astrainteractive.messagebridge.messenger.discord.messaging.DiscordMessageSender
import ru.astrainteractive.messagebridge.messenger.discord.model.DiscordMessageRelevance

internal class MessageEventListener(
    private val relevanceMapper: DiscordMessageRelevanceMapper,
    private val commandMapper: DiscordCommandMapper,
    private val commandHandler: DiscordCommandHandler,
    private val replyMapper: DiscordReplyMapper,
    private val messageSender: DiscordMessageSender,
    private val messageInterceptors: List<MessageInterceptor<MessageReceivedEvent>>,
    private val bEventConsumer: BEventConsumer,
) : CoroutineFeature by CoroutineFeature.IO.withTimings(),
    Logger by JUtiltLogger("MessageBridge-MessageEventListener") {

    private suspend fun runInterceptors(event: MessageReceivedEvent): Interception {
        val interception = messageInterceptors.intercept(event)
        if (interception is Interception.Reply) {
            messageSender.reply(event.message, interception.text)
        }
        return interception
    }

    fun onMessageReceived(event: MessageReceivedEvent) {
        when (relevanceMapper.map(event)) {
            DiscordMessageRelevance.Relevant -> launch { process(event) }
            DiscordMessageRelevance.PrivateMessage -> launch { runInterceptors(event) }
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
        if (runInterceptors(event) != Interception.Pass) return
        relay(event)
    }

    private suspend fun relay(event: MessageReceivedEvent) {
        bEventConsumer.consume(
            Text.Discord(
                author = event.member?.nickname ?: event.author.name,
                text = event.message.contentRaw,
                authorId = event.author.idLong,
                reply = replyMapper.map(event.message),
            )
        )
    }
}
