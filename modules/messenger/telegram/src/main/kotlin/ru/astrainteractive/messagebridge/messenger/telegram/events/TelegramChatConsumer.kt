package ru.astrainteractive.messagebridge.messenger.telegram.events

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer
import org.telegram.telegrambots.meta.api.objects.Update
import ru.astrainteractive.klibs.kstorage.api.CachedKrate
import ru.astrainteractive.klibs.kstorage.api.getValue
import ru.astrainteractive.klibs.mikro.core.dispatchers.KotlinDispatchers
import ru.astrainteractive.klibs.mikro.core.logging.JUtiltLogger
import ru.astrainteractive.klibs.mikro.core.logging.Logger
import ru.astrainteractive.messagebridge.core.PluginTranslation
import ru.astrainteractive.messagebridge.messaging.internal.BEventChannel
import ru.astrainteractive.messagebridge.messaging.model.Text
import ru.astrainteractive.messagebridge.messenger.telegram.mapping.TelegramCommandMapper
import ru.astrainteractive.messagebridge.messenger.telegram.mapping.TelegramMessageRelevanceMapper
import ru.astrainteractive.messagebridge.messenger.telegram.mapping.TelegramMessageValidatorMapper
import ru.astrainteractive.messagebridge.messenger.telegram.messaging.TelegramMessageSender
import ru.astrainteractive.messagebridge.messenger.telegram.model.MessageRelevance
import ru.astrainteractive.messagebridge.messenger.telegram.model.TelegramMessageValidation

internal class TelegramChatConsumer(
    private val ioScope: CoroutineScope,
    private val dispatchers: KotlinDispatchers,
    translationKrate: CachedKrate<PluginTranslation>,
    private val relevanceChecker: TelegramMessageRelevanceMapper,
    private val validator: TelegramMessageValidatorMapper,
    private val commandParser: TelegramCommandMapper,
    private val commandHandler: TelegramCommandHandler,
    private val messageSender: TelegramMessageSender,
) : LongPollingSingleThreadUpdateConsumer,
    Logger by JUtiltLogger("MessageBridge-TelegramChatConsumer").withoutParentHandlers() {
    private val translation by translationKrate

    override fun consume(update: Update?) {
        update ?: return
        commandHandler.logChatInfo(update)
        when (relevanceChecker.map(update)) {
            MessageRelevance.Relevant -> ioScope.launch(dispatchers.IO) { process(update) }
            MessageRelevance.WrongChat -> info { "#consume update is not from the configured chat" }
            MessageRelevance.NoDate -> info { "#consume message date is null" }
            MessageRelevance.TooOld -> info { "#consume message is too old" }
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
                info { "#consume display name rejected by regex" }
                reply(update, translation.illegalDisplayName.raw)
                delete(update)
            }
        }
    }

    private suspend fun relay(update: Update, valid: TelegramMessageValidation.Valid) {
        val command = commandParser.map(valid.text)
        if (command != null) {
            commandHandler.handle(command, update)
            return
        }
        BEventChannel.consume(
            Text.Telegram(
                author = valid.author,
                text = valid.text,
                authorId = valid.authorId,
            )
        )
    }

    private suspend fun reject(update: Update, reason: () -> String) {
        info(reason)
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
