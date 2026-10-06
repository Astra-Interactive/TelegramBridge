package ru.astrainteractive.messagebridge.messenger.discord.internal

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import ru.astrainteractive.klibs.kstorage.api.CachedKrate
import ru.astrainteractive.klibs.kstorage.api.getValue
import ru.astrainteractive.messagebridge.core.api.config.PluginConfiguration
import ru.astrainteractive.messagebridge.messenger.api.model.MessageRef
import ru.astrainteractive.messagebridge.messenger.api.model.Text

internal class DiscordRelayedMessageCache(
    configKrate: CachedKrate<PluginConfiguration>
) {
    private val config by configKrate
    private val mutex = Mutex()
    private val textByMessage = LinkedHashMap<Long, Text>()
    private val messageByOrigin = HashMap<MessageRef, Long>()

    private fun forgetOldest() {
        val oldest = textByMessage.keys.first()
        val forgotten = textByMessage.remove(oldest) ?: return
        if (messageByOrigin[forgotten.ref] == oldest) {
            messageByOrigin.remove(forgotten.ref)
        }
    }

    suspend fun remember(messageId: Long, text: Text) {
        mutex.withLock {
            textByMessage[messageId] = text
            messageByOrigin[text.ref] = messageId
            while (textByMessage.size > config.jdaConfig.relayedMessageCacheSize) {
                forgetOldest()
            }
        }
    }

    suspend fun find(messageId: Long): Text? {
        return mutex.withLock { textByMessage[messageId] }
    }

    suspend fun copyOf(origin: MessageRef): Long? {
        return mutex.withLock { messageByOrigin[origin] }
    }
}
