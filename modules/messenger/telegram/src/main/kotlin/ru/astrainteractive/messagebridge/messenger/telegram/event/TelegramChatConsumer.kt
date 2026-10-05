package ru.astrainteractive.messagebridge.messenger.telegram.event

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer
import org.telegram.telegrambots.meta.api.objects.Update
import ru.astrainteractive.klibs.kstorage.api.CachedKrate
import ru.astrainteractive.klibs.kstorage.api.getValue
import ru.astrainteractive.klibs.mikro.core.dispatchers.KotlinDispatchers
import ru.astrainteractive.klibs.mikro.core.logging.JUtiltLogger
import ru.astrainteractive.klibs.mikro.core.logging.Logger
import ru.astrainteractive.messagebridge.core.config.PluginTranslation
import ru.astrainteractive.messagebridge.core.mapping.toMessengerText
import ru.astrainteractive.messagebridge.messaging.api.BEventConsumer
import ru.astrainteractive.messagebridge.messaging.api.MessageInterceptor
import ru.astrainteractive.messagebridge.messaging.api.intercept
import ru.astrainteractive.messagebridge.messaging.model.Interception
import ru.astrainteractive.messagebridge.messaging.model.Text
import ru.astrainteractive.messagebridge.messenger.telegram.command.TelegramCommandHandler
import ru.astrainteractive.messagebridge.messenger.telegram.command.TelegramCommandMapper
import ru.astrainteractive.messagebridge.messenger.telegram.internal.TelegramMessageRelevanceMapper
import ru.astrainteractive.messagebridge.messenger.telegram.internal.TelegramMessageSender
import ru.astrainteractive.messagebridge.messenger.telegram.internal.TelegramMessageValidatorMapper
import ru.astrainteractive.messagebridge.messenger.telegram.internal.TelegramReplyMapper
import ru.astrainteractive.messagebridge.messenger.telegram.model.MessageRelevance
import ru.astrainteractive.messagebridge.messenger.telegram.model.TelegramMessageValidation

internal class TelegramChatConsumer(
    private val ioScope: CoroutineScope,
    private val dispatchers: KotlinDispatchers,
    translationKrate: CachedKrate<PluginTranslation>,
    private val relevanceChecker: TelegramMessageRelevanceMapper,
    private val validator: TelegramMessageValidatorMapper,
    private val replyMapper: TelegramReplyMapper,
    private val commandParser: TelegramCommandMapper,
    private val commandHandler: TelegramCommandHandler,
    private val messageSender: TelegramMessageSender,
    private val bEventConsumer: BEventConsumer,
    private val messageInterceptors: List<MessageInterceptor<Update>>,
) : LongPollingSingleThreadUpdateConsumer,
    Logger by JUtiltLogger("MessageBridge-TelegramChatConsumer") {
    private val translation by translationKrate

    override fun consume(update: Update?) {
        update ?: return
        commandHandler.logChatInfo(update)
        when (relevanceChecker.map(update)) {
            MessageRelevance.Relevant -> ioScope.launch(dispatchers.IO) { process(update) }
            MessageRelevance.WrongChat -> verbose { "#consume update is not from the configured chat" }
            MessageRelevance.NoDate -> verbose { "#consume message date is null" }
            MessageRelevance.TooOld -> verbose { "#consume message is too old" }
            MessageRelevance.WrongTopic -> Unit
        }
    }

    private suspend fun process(update: Update) {
        when (val validation = validator.map(update)) {
            is TelegramMessageValidation.Valid -> relay(update, validation)
            TelegramMessageValidation.NoAuthor -> reject(update) { "#consume author name is null" }
            TelegramMessageValidation.NoText -> reject(update) { "#consume text is null" }
            TelegramMessageValidation.TooLong -> reject(update) { "#consume message exceeds max length" }
            TelegramMessageValidation.IllegalDisplayName -> {
                verbose { "#consume display name rejected by regex" }
                reply(update, translation.chat.illegalDisplayName.toMessengerText())
                delete(update)
            }
        }
    }

    private suspend fun answer(update: Update, text: String) {
        val message = update.message ?: return
        messageSender.send(message.chatId.toString(), text, message.replyToMessage?.messageId)
    }

    private suspend fun relay(update: Update, valid: TelegramMessageValidation.Valid) {
        val command = commandParser.map(valid.text)
        if (command != null) {
            commandHandler.handle(command, update)
            return
        }
        val interception = messageInterceptors.intercept(update)
        if (interception is Interception.Reply) {
            answer(update, interception.text)
        }
        if (interception != Interception.Pass) return
        bEventConsumer.consume(
            Text.Telegram(
                author = valid.author,
                text = valid.text,
                authorId = valid.authorId,
                reply = update.message?.let { message -> replyMapper.map(message) },
            )
        )
    }

    private suspend fun reject(update: Update, reason: () -> String) {
        verbose(reason)
        delete(update)
    }

    private suspend fun reply(update: Update, text: String) {
        val message = update.message ?: return
        messageSender.send(message.chatId.toString(), text, replyToMessageId = message.messageId)
    }

    private suspend fun delete(update: Update) {
        val message = update.message ?: return
        messageSender.delete(message.chatId.toString(), message.messageId)
    }
}
