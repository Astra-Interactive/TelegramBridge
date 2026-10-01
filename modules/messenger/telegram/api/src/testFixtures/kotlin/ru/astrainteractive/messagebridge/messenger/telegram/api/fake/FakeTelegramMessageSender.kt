package ru.astrainteractive.messagebridge.messenger.telegram.api.fake

import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage
import ru.astrainteractive.messagebridge.messenger.telegram.api.api.TelegramBotApi
import ru.astrainteractive.messagebridge.messenger.telegram.api.api.TelegramMessageSender

class FakeTelegramMessageSender(
    private val botApi: TelegramBotApi
) : TelegramMessageSender {
    override suspend fun send(chatId: String, text: String, replyToMessageId: Int?) {
        botApi.execute(SendMessage(chatId, text).apply { this.replyToMessageId = replyToMessageId })
    }

    override suspend fun delete(chatId: String, messageId: Int) {
        botApi.execute(DeleteMessage(chatId, messageId))
    }
}
