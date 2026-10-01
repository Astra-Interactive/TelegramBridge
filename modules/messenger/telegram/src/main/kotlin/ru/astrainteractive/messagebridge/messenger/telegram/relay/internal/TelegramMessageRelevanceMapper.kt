package ru.astrainteractive.messagebridge.messenger.telegram.relay.internal

import kotlinx.coroutines.flow.StateFlow
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.message.Message
import ru.astrainteractive.messagebridge.core.api.config.PluginConfiguration
import ru.astrainteractive.messagebridge.messenger.telegram.relay.model.MessageRelevance
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

internal class TelegramMessageRelevanceMapper(
    private val configFlow: StateFlow<PluginConfiguration>,
    private val clock: Clock,
) {
    private val tgConfig: PluginConfiguration.TelegramConfig
        get() = configFlow.value.tgConfig

    private fun isInConfiguredTopic(message: Message): Boolean {
        if (tgConfig.topicID.isBlank()) return !message.isTopicMessage()
        val threadId = message.messageThreadId ?: message.replyToMessage?.messageId
        return tgConfig.topicID == threadId?.toString()
    }

    fun map(update: Update): MessageRelevance {
        val message = update.message
        if (message == null || tgConfig.chatID != message.chatId?.toString()) {
            return MessageRelevance.WrongChat
        }
        val date = message.date?.toLong() ?: return MessageRelevance.NoDate
        val age = clock.now() - Instant.fromEpochSeconds(date)
        if (age > MAX_MESSAGE_AGE) {
            return MessageRelevance.TooOld
        }
        if (!isInConfiguredTopic(message)) {
            return MessageRelevance.WrongTopic
        }
        return MessageRelevance.Relevant
    }

    private companion object {
        val MAX_MESSAGE_AGE = 10.seconds
    }
}
