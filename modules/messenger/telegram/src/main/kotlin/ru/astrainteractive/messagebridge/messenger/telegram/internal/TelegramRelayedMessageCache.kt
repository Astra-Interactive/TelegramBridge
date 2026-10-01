package ru.astrainteractive.messagebridge.messenger.telegram.internal

import ru.astrainteractive.messagebridge.messaging.model.Text

internal class TelegramRelayedMessageCache(
    private val capacity: Int
) {
    private val lock = Any()
    private val textByMessage = LinkedHashMap<MessageKey, Text>()

    fun remember(chatId: Long, messageId: Int, text: Text) {
        synchronized(lock) {
            textByMessage[MessageKey(chatId = chatId, messageId = messageId)] = text
            if (textByMessage.size > capacity) {
                textByMessage.remove(textByMessage.keys.first())
            }
        }
    }

    fun find(chatId: Long, messageId: Int): Text? {
        return synchronized(lock) {
            textByMessage[MessageKey(chatId = chatId, messageId = messageId)]
        }
    }

    private data class MessageKey(
        val chatId: Long,
        val messageId: Int
    )
}
