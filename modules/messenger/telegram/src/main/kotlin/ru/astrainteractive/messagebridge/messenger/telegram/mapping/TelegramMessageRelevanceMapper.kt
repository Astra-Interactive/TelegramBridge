package ru.astrainteractive.messagebridge.messenger.telegram.mapping

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.telegram.telegrambots.meta.api.objects.Update
import ru.astrainteractive.klibs.kstorage.api.CachedKrate
import ru.astrainteractive.klibs.kstorage.api.getValue
import ru.astrainteractive.messagebridge.core.PluginConfiguration
import kotlin.time.Duration.Companion.seconds
import ru.astrainteractive.messagebridge.messenger.telegram.model.MessageRelevance

/**
 * Routes incoming updates: decides whether one targets the configured bridge chat/topic and is
 * recent enough to act on. It does not inspect the message content — that is validation's concern.
 */
internal class TelegramMessageRelevanceMapper(
    configKrate: CachedKrate<PluginConfiguration>,
    private val clock: Clock = Clock.System,
) {
    private val config by configKrate
    private val tgConfig: PluginConfiguration.TelegramConfig
        get() = config.tgConfig

    fun map(update: Update): MessageRelevance {
        val message = update.message
        if (tgConfig.chatID != message?.chatId?.toString()) {
            return MessageRelevance.WrongChat
        }
        val date = message.date?.toLong() ?: return MessageRelevance.NoDate
        val age = clock.now().minus(Instant.fromEpochSeconds(date))
        if (age > MAX_MESSAGE_AGE) {
            return MessageRelevance.TooOld
        }
        val replyMessageId = message.replyToMessage?.messageId?.toString()
        val messageThreadId = message.messageThreadId?.toString()
        if (tgConfig.topicID != (messageThreadId ?: replyMessageId)) {
            return MessageRelevance.WrongTopic
        }
        return MessageRelevance.Relevant
    }

    @Suppress("MagicNumber")
    private companion object {
        val MAX_MESSAGE_AGE = 10.seconds
    }
}
