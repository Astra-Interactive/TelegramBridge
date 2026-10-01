package ru.astrainteractive.messagebridge.messenger.telegram.impl.relay.mapping

import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.message.Message
import ru.astrainteractive.klibs.kstorage.api.CachedKrate
import ru.astrainteractive.klibs.kstorage.api.getValue
import ru.astrainteractive.messagebridge.core.api.config.PluginTranslation
import ru.astrainteractive.messagebridge.core.api.util.toMessengerText
import ru.astrainteractive.messagebridge.messenger.telegram.impl.relay.model.TelegramAuthor

internal class TelegramAuthorMapper(
    translationKrate: CachedKrate<PluginTranslation>,
) {
    private val translation by translationKrate

    private fun String.toFixedName(): String {
        return trim()
            .replace("\n", "")
            .replace("\t", "")
            .take(MAX_NAME_LENGTH)
    }

    private fun author(userName: String?, firstName: String?, lastName: String?): TelegramAuthor {
        if (userName != null) return TelegramAuthor.Username(userName.toFixedName())
        val shownFirstName = firstName ?: translation.telegram.anonymousAuthor.toMessengerText()
        return TelegramAuthor.DisplayName("$shownFirstName ${lastName.orEmpty()}".toFixedName())
    }

    fun map(update: Update): TelegramAuthor? {
        val message = update.message ?: return null
        return map(message)
    }

    fun map(message: Message): TelegramAuthor? {
        message.senderChat?.let { chat ->
            return author(chat.userName, chat.firstName, chat.lastName)
        }
        message.from?.let { user ->
            return author(user.userName, user.firstName, user.lastName)
        }
        return null
    }

    private companion object {
        const val MAX_NAME_LENGTH = 16
    }
}
