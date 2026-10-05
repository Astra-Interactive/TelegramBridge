package ru.astrainteractive.messagebridge.messenger.telegram.internal

import org.telegram.telegrambots.meta.api.objects.message.Message
import ru.astrainteractive.klibs.kstorage.api.CachedKrate
import ru.astrainteractive.klibs.kstorage.api.getValue
import ru.astrainteractive.messagebridge.core.config.PluginConfiguration
import ru.astrainteractive.messagebridge.messenger.api.model.Text
import ru.astrainteractive.messagebridge.messenger.telegram.mapping.TelegramAuthorMapper

internal class TelegramReplyMapper(
    configKrate: CachedKrate<PluginConfiguration>,
    private val authorMapper: TelegramAuthorMapper,
    private val relayedMessageCache: TelegramRelayedMessageCache,
) {
    private val config by configKrate

    private fun isTopicStart(replied: Message): Boolean {
        return replied.forumTopicCreated != null || replied.messageId.toString() == config.tgConfig.topicID
    }

    private suspend fun relayedReply(chatId: Long, replied: Message): Text.Reply? {
        val relayed = relayedMessageCache.find(chatId = chatId, messageId = replied.messageId) ?: return null
        return Text.Reply(
            author = relayed.author,
            authorId = null,
            text = relayed.text
        )
    }

    private fun authoredReply(replied: Message): Text.Reply? {
        val author = authorMapper.map(replied) ?: return null
        return Text.Reply(
            author = author.name,
            authorId = replied.from?.id,
            text = replied.text ?: replied.caption.orEmpty()
        )
    }

    suspend fun map(message: Message): Text.Reply? {
        val replied = message.replyToMessage ?: return null
        if (isTopicStart(replied)) return null
        val reply = relayedReply(message.chatId, replied) ?: authoredReply(replied) ?: return null
        val quote = message.quote?.text ?: return reply
        return reply.copy(text = quote)
    }
}
