package ru.astrainteractive.messagebridge.messenger.telegram.relay.internal

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import ru.astrainteractive.messagebridge.messenger.api.model.Text

internal class TelegramRelayedMessageCache(
    private val capacity: Int
) {
    private val mutex = Mutex()
    private val textByMessage = LinkedHashMap<MessageKey, Text>()

    suspend fun remember(chatId: Long, messageId: Int, text: Text) {
        val key = MessageKey(chatId = chatId, messageId = messageId)
        mutex.withLock {
            textByMessage[key] = text
            if (textByMessage.size > capacity) {
                textByMessage.remove(textByMessage.keys.first())
            }
        }
    }

    suspend fun find(chatId: Long, messageId: Int): Text? {
        val key = MessageKey(chatId = chatId, messageId = messageId)
        return mutex.withLock { textByMessage[key] }
    }

    private data class MessageKey(
        val chatId: Long,
        val messageId: Int
    )
}
