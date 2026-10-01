package ru.astrainteractive.messagebridge.messenger.telegram.mapping

import org.telegram.telegrambots.meta.api.objects.message.Message
import ru.astrainteractive.klibs.kstorage.api.CachedKrate
import ru.astrainteractive.klibs.kstorage.api.getValue
import ru.astrainteractive.messagebridge.core.PluginConfiguration
import ru.astrainteractive.messagebridge.messaging.model.Text
import ru.astrainteractive.messagebridge.messenger.telegram.internal.TelegramRelayedMessageCache

internal class TelegramReplyMapper(
    configKrate: CachedKrate<PluginConfiguration>,
    private val authorMapper: TelegramAuthorMapper,
    private val relayedMessageCache: TelegramRelayedMessageCache,
) {
    private val config by configKrate

    /**
     * Telegram sets the first message of a forum topic as the replied message of every message in the topic, so
     * replying to it, or to the configured topic message, is not a reply the author chose.
     */
    private fun isTopicStart(replied: Message): Boolean {
        return replied.forumTopicCreated != null || replied.messageId.toString() == config.tgConfig.topicID
    }

    private fun relayedReply(chatId: Long, replied: Message): Text.Reply? {
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

    fun map(message: Message): Text.Reply? {
        val replied = message.replyToMessage ?: return null
        if (isTopicStart(replied)) return null
        val reply = relayedReply(message.chatId, replied) ?: authoredReply(replied) ?: return null
        val quote = message.quote?.text ?: return reply
        return reply.copy(text = quote)
    }
}
