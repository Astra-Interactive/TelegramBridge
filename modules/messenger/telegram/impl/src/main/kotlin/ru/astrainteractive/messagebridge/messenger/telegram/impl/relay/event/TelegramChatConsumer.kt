package ru.astrainteractive.messagebridge.messenger.telegram.impl.relay.event

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer
import org.telegram.telegrambots.meta.api.objects.Update
import ru.astrainteractive.klibs.kstorage.api.CachedKrate
import ru.astrainteractive.klibs.kstorage.api.getValue
import ru.astrainteractive.klibs.mikro.core.logging.JUtiltLogger
import ru.astrainteractive.klibs.mikro.core.logging.Logger
import ru.astrainteractive.messagebridge.core.api.config.PluginTranslation
import ru.astrainteractive.messagebridge.core.api.util.toMessengerText
import ru.astrainteractive.messagebridge.messenger.api.api.BEventConsumer
import ru.astrainteractive.messagebridge.messenger.api.model.BEvent
import ru.astrainteractive.messagebridge.messenger.telegram.api.api.TelegramMessageSender
import ru.astrainteractive.messagebridge.messenger.telegram.api.api.TelegramUpdateInterceptor
import ru.astrainteractive.messagebridge.messenger.telegram.impl.command.internal.TelegramCommandHandler
import ru.astrainteractive.messagebridge.messenger.telegram.impl.command.internal.TelegramCommandParser
import ru.astrainteractive.messagebridge.messenger.telegram.impl.relay.internal.TelegramMessageRelevanceMapper
import ru.astrainteractive.messagebridge.messenger.telegram.impl.relay.internal.TelegramMessageValidator
import ru.astrainteractive.messagebridge.messenger.telegram.impl.relay.internal.TelegramReplyMapper
import ru.astrainteractive.messagebridge.messenger.telegram.impl.relay.model.MessageRelevance
import ru.astrainteractive.messagebridge.messenger.telegram.impl.relay.model.TelegramMessageValidation

internal class TelegramChatConsumer(
    private val scope: CoroutineScope,
    translationKrate: CachedKrate<PluginTranslation>,
    private val relevanceMapper: TelegramMessageRelevanceMapper,
    private val validator: TelegramMessageValidator,
    private val replyMapper: TelegramReplyMapper,
    private val commandParser: TelegramCommandParser,
    private val commandHandler: TelegramCommandHandler,
    private val messageSender: TelegramMessageSender,
    private val eventChannel: BEventConsumer,
    private val updateInterceptors: () -> List<TelegramUpdateInterceptor>,
) : LongPollingSingleThreadUpdateConsumer,
    Logger by JUtiltLogger("MessageBridge-TelegramChatConsumer") {
    private val translation by translationKrate

    private suspend fun reply(update: Update, text: String) {
        val message = update.message ?: return
        messageSender.send(message.chatId.toString(), text, replyToMessageId = message.messageId)
    }

    private suspend fun delete(update: Update) {
        val message = update.message ?: return
        messageSender.delete(message.chatId.toString(), message.messageId)
    }

    private suspend fun relay(update: Update, valid: TelegramMessageValidation.Valid) {
        val command = commandParser.map(valid.text)
        if (command != null) {
            commandHandler.handle(command, update)
            return
        }
        eventChannel.consume(
            BEvent.Text.Telegram(
                author = valid.author,
                text = valid.text,
                authorId = valid.authorId,
                reply = update.message?.let { message -> replyMapper.map(message) },
            )
        )
    }

    private suspend fun process(update: Update) {
        when (val validation = validator.map(update)) {
            is TelegramMessageValidation.Valid -> relay(update, validation)
            TelegramMessageValidation.NoAuthor -> verbose { "#process author name is null" }
            TelegramMessageValidation.NoText -> verbose { "#process text is null" }
            TelegramMessageValidation.TooLong -> {
                verbose { "#process message exceeds max length" }
                delete(update)
            }

            TelegramMessageValidation.IllegalDisplayName -> {
                verbose { "#process display name rejected by regex" }
                reply(update, translation.chat.illegalDisplayName.toMessengerText())
                delete(update)
            }
        }
    }

    override fun consume(update: Update?) {
        update ?: return
        if (updateInterceptors.invoke().any { interceptor -> interceptor.intercept(update) }) return
        when (relevanceMapper.map(update)) {
            MessageRelevance.Relevant -> scope.launch { process(update) }
            MessageRelevance.WrongChat -> verbose { "#consume update is not from the configured chat" }
            MessageRelevance.NoDate -> verbose { "#consume message date is null" }
            MessageRelevance.TooOld -> verbose { "#consume message is too old" }
            MessageRelevance.WrongTopic -> Unit
        }
    }
}
