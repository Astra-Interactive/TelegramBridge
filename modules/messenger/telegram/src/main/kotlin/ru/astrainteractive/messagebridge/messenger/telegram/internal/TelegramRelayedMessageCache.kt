package ru.astrainteractive.messagebridge.messenger.telegram.internal

import ru.astrainteractive.messagebridge.messaging.model.Text

/**
 * Remembers the [Text] behind each message the bot relayed to Telegram, so a reply to it names the player who
 * wrote it instead of the bot. Keeps the [capacity] latest messages in memory only: after a restart, a reply to an
 * older message names the bot. Safe to call from any thread.
 */
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
