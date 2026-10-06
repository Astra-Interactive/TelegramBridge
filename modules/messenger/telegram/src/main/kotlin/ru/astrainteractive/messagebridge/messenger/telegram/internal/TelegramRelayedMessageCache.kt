package ru.astrainteractive.messagebridge.messenger.telegram.internal

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import ru.astrainteractive.klibs.kstorage.api.CachedKrate
import ru.astrainteractive.klibs.kstorage.api.getValue
import ru.astrainteractive.messagebridge.core.api.config.PluginConfiguration
import ru.astrainteractive.messagebridge.messenger.api.model.MessageRef
import ru.astrainteractive.messagebridge.messenger.api.model.Text

internal class TelegramRelayedMessageCache(
    configKrate: CachedKrate<PluginConfiguration>
) {
    private val config by configKrate
    private val mutex = Mutex()
    private val textByMessage = LinkedHashMap<MessageKey, Text>()
    private val messageByOrigin = HashMap<MessageRef, MessageKey>()

    private fun forgetOldest() {
        val oldest = textByMessage.keys.first()
        val forgotten = textByMessage.remove(oldest) ?: return
        if (messageByOrigin[forgotten.ref] == oldest) {
            messageByOrigin.remove(forgotten.ref)
        }
    }

    suspend fun remember(chatId: Long, messageId: Int, text: Text) {
        val key = MessageKey(chatId = chatId, messageId = messageId)
        mutex.withLock {
            textByMessage[key] = text
            messageByOrigin[text.ref] = key
            while (textByMessage.size > config.tgConfig.relayedMessageCacheSize) {
                forgetOldest()
            }
        }
    }

    suspend fun find(chatId: Long, messageId: Int): Text? {
        val key = MessageKey(chatId = chatId, messageId = messageId)
        return mutex.withLock { textByMessage[key] }
    }

    suspend fun copyOf(origin: MessageRef, chatId: Long): Int? {
        return mutex.withLock { messageByOrigin[origin] }
            ?.takeIf { key -> key.chatId == chatId }
            ?.messageId
    }

    private data class MessageKey(
        val chatId: Long,
        val messageId: Int
    )
}
