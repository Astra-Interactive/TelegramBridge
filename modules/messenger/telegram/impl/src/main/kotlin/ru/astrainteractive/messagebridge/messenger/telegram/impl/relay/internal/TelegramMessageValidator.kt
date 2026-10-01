package ru.astrainteractive.messagebridge.messenger.telegram.impl.relay.internal

import kotlinx.coroutines.flow.StateFlow
import org.telegram.telegrambots.meta.api.objects.Update
import ru.astrainteractive.messagebridge.core.api.config.PluginConfiguration
import ru.astrainteractive.messagebridge.messenger.telegram.impl.relay.model.TelegramAuthor
import ru.astrainteractive.messagebridge.messenger.telegram.impl.relay.model.TelegramMessageValidation

internal class TelegramMessageValidator(
    private val configFlow: StateFlow<PluginConfiguration>,
    private val authorMapper: TelegramAuthorMapper,
) {
    private val tgConfig: PluginConfiguration.TelegramConfig
        get() = configFlow.value.tgConfig

    private fun isDisplayNameAllowed(name: String): Boolean {
        return tgConfig.displayNameRegex.toRegex().matches(name)
    }

    fun map(update: Update): TelegramMessageValidation {
        val author = authorMapper.map(update) ?: return TelegramMessageValidation.NoAuthor
        if (author is TelegramAuthor.DisplayName && !isDisplayNameAllowed(author.name)) {
            return TelegramMessageValidation.IllegalDisplayName
        }
        val text = update.message?.text
        if (text.isNullOrBlank()) {
            return TelegramMessageValidation.NoText
        }
        if (text.length > tgConfig.maxTelegramMessageLength) {
            return TelegramMessageValidation.TooLong
        }
        return TelegramMessageValidation.Valid(
            author = author.name,
            text = text,
            authorId = update.message?.from?.id ?: UNKNOWN_AUTHOR_ID,
        )
    }

    private companion object {
        const val UNKNOWN_AUTHOR_ID = 0L
    }
}
