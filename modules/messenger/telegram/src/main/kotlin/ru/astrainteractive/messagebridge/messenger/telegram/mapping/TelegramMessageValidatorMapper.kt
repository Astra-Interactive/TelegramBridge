package ru.astrainteractive.messagebridge.messenger.telegram.mapping

import org.telegram.telegrambots.meta.api.objects.Update
import ru.astrainteractive.klibs.kstorage.api.CachedKrate
import ru.astrainteractive.klibs.kstorage.api.getValue
import ru.astrainteractive.messagebridge.core.PluginConfiguration
import ru.astrainteractive.messagebridge.messenger.telegram.model.TelegramAuthor
import ru.astrainteractive.messagebridge.messenger.telegram.model.TelegramMessageValidation

internal class TelegramMessageValidatorMapper(
    configKrate: CachedKrate<PluginConfiguration>,
    private val authorMapper: TelegramAuthorMapper,
) {
    private val config by configKrate
    private val tgConfig: PluginConfiguration.TelegramConfig
        get() = config.tgConfig

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
            authorId = update.message?.from?.id ?: 0L,
        )
    }

    private fun isDisplayNameAllowed(name: String): Boolean =
        tgConfig.displayNameRegex.toRegex().matches(name)
}
