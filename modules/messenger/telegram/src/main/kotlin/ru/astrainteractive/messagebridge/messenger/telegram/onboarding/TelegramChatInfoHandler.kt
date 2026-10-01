package ru.astrainteractive.messagebridge.messenger.telegram.onboarding

import org.telegram.telegrambots.meta.api.objects.message.Message
import ru.astrainteractive.klibs.kstorage.api.CachedKrate
import ru.astrainteractive.klibs.kstorage.api.getValue
import ru.astrainteractive.klibs.mikro.core.logging.Logger
import ru.astrainteractive.messagebridge.core.PluginTranslation
import ru.astrainteractive.messagebridge.core.mapping.toMessengerText
import ru.astrainteractive.messagebridge.messenger.telegram.request.TelegramMessageSender

/** Replies to `/minfo` with the ids a chat is set up by. */
internal class TelegramChatInfoHandler(
    private val messageSender: TelegramMessageSender,
    translationKrate: CachedKrate<PluginTranslation>,
    logger: Logger,
) : Logger by logger {
    private val translation by translationKrate

    suspend fun sendChatInfo(message: Message) {
        val chatId = message.chatId.toString()
        val topicId = message.topicIdOrNull() ?: NO_TOPIC
        val isForum = message.chat.isForum == true
        info {
            "#sendChatInfo chat_id: $chatId; topic_id: $topicId; type: ${message.chat.type}; forum: $isForum; " +
                "replied message: ${message.replyToMessage?.messageId}"
        }
        val text = translation.telegram.chatInfo.message(
            chatId = chatId,
            topicId = topicId,
            type = message.chat.type.orEmpty(),
            isForum = isForum
        )
        messageSender.send(chatId, text.toMessengerText(), replyToMessageId = message.messageId)
    }

    private companion object {
        /** The argument `/mb telegram topic` takes for no topic, so it is not translated. */
        const val NO_TOPIC = "none"
    }
}
