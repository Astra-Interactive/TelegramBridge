package ru.astrainteractive.messagebridge.messenger.telegram.message.mapping

import org.telegram.telegrambots.meta.api.objects.message.Message
import ru.astrainteractive.klibs.kstorage.api.CachedKrate
import ru.astrainteractive.klibs.kstorage.api.getValue
import ru.astrainteractive.messagebridge.core.api.config.PluginConfiguration
import ru.astrainteractive.messagebridge.core.api.config.PluginTranslation
import ru.astrainteractive.messagebridge.core.api.mapping.toMessengerText
import ru.astrainteractive.messagebridge.messenger.api.model.MessageRef
import ru.astrainteractive.messagebridge.messenger.api.model.Text
import ru.astrainteractive.messagebridge.messenger.telegram.internal.TelegramRelayedMessageCache

internal class TelegramReplyMapper(
    configKrate: CachedKrate<PluginConfiguration>,
    translationKrate: CachedKrate<PluginTranslation>,
    private val authorMapper: TelegramAuthorMapper,
    private val relayedMessageCache: TelegramRelayedMessageCache,
) {
    private val config by configKrate
    private val translation by translationKrate
    private val botId: Long?
        get() = config.tgConfig.token.substringBefore(':').toLongOrNull()

    private fun isTopicStart(replied: Message): Boolean {
        return replied.forumTopicCreated != null || replied.messageId.toString() == config.tgConfig.topicID
    }

    private suspend fun relayedReply(chatId: Long, replied: Message): Text.Reply? {
        val relayed = relayedMessageCache.find(chatId = chatId, messageId = replied.messageId) ?: return null
        return Text.Reply(
            author = relayed.author,
            authorId = null,
            text = relayed.text,
            target = relayed.ref
        )
    }

    private fun isOwnMessage(replied: Message): Boolean {
        val ownId = botId ?: return false
        return replied.from?.id == ownId
    }

    private fun authoredReply(replied: Message): Text.Reply? {
        val target = MessageRef.Telegram(chatId = replied.chatId, messageId = replied.messageId)
        val text = replied.text ?: replied.caption.orEmpty()
        if (isOwnMessage(replied)) {
            return Text.Reply(
                author = translation.chat.replyServer.toMessengerText(),
                authorId = null,
                text = text,
                target = target
            )
        }
        val author = authorMapper.map(replied) ?: return null
        return Text.Reply(
            author = author.name,
            authorId = replied.from?.id,
            text = text,
            target = target
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
