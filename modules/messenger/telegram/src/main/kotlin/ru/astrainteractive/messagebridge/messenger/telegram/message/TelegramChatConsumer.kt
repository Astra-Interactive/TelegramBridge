package ru.astrainteractive.messagebridge.messenger.telegram.message

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer
import org.telegram.telegrambots.meta.api.objects.Update
import ru.astrainteractive.klibs.mikro.core.dispatchers.KotlinDispatchers
import ru.astrainteractive.klibs.mikro.core.logging.JUtiltLogger
import ru.astrainteractive.klibs.mikro.core.logging.Logger
import ru.astrainteractive.messagebridge.messenger.telegram.message.mapping.TelegramMessageRelevanceMapper
import ru.astrainteractive.messagebridge.messenger.telegram.message.mapping.TelegramMessageValidatorMapper
import ru.astrainteractive.messagebridge.messenger.telegram.message.model.MessageRelevance
import ru.astrainteractive.messagebridge.messenger.telegram.message.model.TelegramMessageValidation

internal class TelegramChatConsumer(
    private val ioScope: CoroutineScope,
    private val dispatchers: KotlinDispatchers,
    private val relevanceChecker: TelegramMessageRelevanceMapper,
    private val validator: TelegramMessageValidatorMapper,
    private val commandHandler: TelegramCommandHandler,
    private val messageRelay: TelegramMessageRelay,
    private val messageRejector: TelegramMessageRejector,
) : LongPollingSingleThreadUpdateConsumer,
    Logger by JUtiltLogger("MessageBridge-TelegramChatConsumer") {

    private suspend fun dispatchMessage(update: Update) {
        when (val validation = validator.toValidation(update)) {
            is TelegramMessageValidation.Valid -> messageRelay.relay(update, validation)
            TelegramMessageValidation.NoAuthor -> messageRejector.reject(update) { "#consume author name is null" }
            TelegramMessageValidation.NoText -> messageRejector.reject(update) { "#consume text is null" }
            TelegramMessageValidation.TooLong -> {
                messageRejector.reject(update) { "#consume message exceeds max length" }
            }

            TelegramMessageValidation.IllegalDisplayName -> messageRejector.rejectIllegalDisplayName(update)
        }
    }

    override fun consume(update: Update?) {
        update ?: return
        commandHandler.logChatInfo(update)
        when (relevanceChecker.toRelevance(update)) {
            MessageRelevance.Relevant -> ioScope.launch(dispatchers.IO) { dispatchMessage(update) }
            MessageRelevance.WrongChat -> verbose { "#consume update is not from the configured chat" }
            MessageRelevance.NoDate -> verbose { "#consume message date is null" }
            MessageRelevance.TooOld -> verbose { "#consume message is too old" }
            MessageRelevance.WrongTopic -> Unit
        }
    }
}
