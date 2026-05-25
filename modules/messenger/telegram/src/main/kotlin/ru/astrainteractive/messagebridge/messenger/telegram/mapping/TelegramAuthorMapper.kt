package ru.astrainteractive.messagebridge.messenger.telegram.mapping

import org.telegram.telegrambots.meta.api.objects.Update
import ru.astrainteractive.messagebridge.messenger.telegram.model.TelegramAuthor

/**
 * Extracts and sanitizes the [ru.astrainteractive.messagebridge.messenger.telegram.model.TelegramAuthor] of a message.
 */
internal class TelegramAuthorMapper {

    fun map(update: Update): TelegramAuthor? {
        val message = update.message ?: return null
        message.senderChat?.let { chat ->
            return author(chat.userName, chat.firstName, chat.lastName)
        }
        message.from?.let { user ->
            return author(user.userName, user.firstName, user.lastName)
        }
        return null
    }

    private fun author(userName: String?, firstName: String?, lastName: String?): TelegramAuthor {
        userName?.let { return TelegramAuthor.Username(it.toFixedName()) }
        val displayName = "${firstName ?: ANONYMOUS} ${lastName ?: ""}".toFixedName()
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
        const val ANONYMOUS = "Анонимус"
        const val MAX_NAME_LENGTH = 16
    }
}
