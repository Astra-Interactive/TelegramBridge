package ru.astrainteractive.messagebridge.messenger.telegram.api.api

interface TelegramMessageSender {
    suspend fun send(chatId: String, text: String, replyToMessageId: Int?)

    suspend fun delete(chatId: String, messageId: Int)
}
