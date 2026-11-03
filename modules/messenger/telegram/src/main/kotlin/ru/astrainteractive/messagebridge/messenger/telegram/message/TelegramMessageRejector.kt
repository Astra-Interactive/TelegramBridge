package ru.astrainteractive.messagebridge.messenger.telegram.message

import org.telegram.telegrambots.meta.api.objects.Update
import ru.astrainteractive.klibs.kstorage.api.CachedKrate
import ru.astrainteractive.klibs.kstorage.api.getValue
import ru.astrainteractive.klibs.mikro.core.logging.JUtiltLogger
import ru.astrainteractive.klibs.mikro.core.logging.Logger
import ru.astrainteractive.messagebridge.core.api.config.model.PluginTranslation
import ru.astrainteractive.messagebridge.core.api.util.toMessengerText

internal class TelegramMessageRejector(
    translationKrate: CachedKrate<PluginTranslation>,
    private val messageSender: TelegramMessageSender,
) : Logger by JUtiltLogger("MessageBridge-TelegramChatConsumer") {
    private val translation by translationKrate

    private suspend fun delete(update: Update) {
        val message = update.message ?: return
        messageSender.delete(message.chatId.toString(), message.messageId)
    }

    private suspend fun reply(update: Update, text: String) {
        val message = update.message ?: return
        messageSender.send(
            message.chatId.toString(),
            text,
            replyToMessageId = message.messageId
        )
    }

    suspend fun reject(update: Update, reason: () -> String) {
        verbose(reason)
        delete(update)
    }

    suspend fun rejectIllegalDisplayName(update: Update) {
        verbose { "#consume display name rejected by regex" }
        reply(update, translation.chat.illegalDisplayName.toMessengerText())
        delete(update)
    }
}
