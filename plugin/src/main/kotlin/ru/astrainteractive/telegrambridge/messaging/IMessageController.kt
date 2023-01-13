package ru.astrainteractive.telegrambridge.messaging

interface IMessageController {
    suspend fun sendToMinecraft(message: Message)
    suspend fun sendToDiscord(message: Message)
    suspend fun sendToTelegram(message: Message)
}