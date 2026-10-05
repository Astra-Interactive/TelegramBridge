package ru.astrainteractive.messagebridge.messenger.telegram.mapping

import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.message.Message
import ru.astrainteractive.klibs.kstorage.api.CachedKrate
import ru.astrainteractive.klibs.kstorage.api.getValue
import ru.astrainteractive.messagebridge.core.api.config.PluginTranslation
import ru.astrainteractive.messagebridge.core.api.mapping.toMessengerText
import ru.astrainteractive.messagebridge.messenger.telegram.model.TelegramAuthor

internal class TelegramAuthorMapper(
    translationKrate: CachedKrate<PluginTranslation>,
) {
    private val translation by translationKrate

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

    private fun author(userName: String?, firstName: String?, lastName: String?): TelegramAuthor {
        userName?.let { name -> return TelegramAuthor.Username(name.toFixedName()) }
        val shownFirstName = firstName ?: translation.chat.anonymousAuthor.toMessengerText()
        val displayName = "$shownFirstName ${lastName ?: ""}".toFixedName()
        return TelegramAuthor.DisplayName(displayName)
    }

    private fun String.toFixedName(): String {
        val name = trim()
            .replace("\n", "")
            .replace("\t", "")
        return if (name.length > MAX_NAME_LENGTH) name.substring(0, MAX_NAME_LENGTH) else name
    }

    @Suppress("MagicNumber")
    private companion object {
        const val MAX_NAME_LENGTH = 16
    }
}
