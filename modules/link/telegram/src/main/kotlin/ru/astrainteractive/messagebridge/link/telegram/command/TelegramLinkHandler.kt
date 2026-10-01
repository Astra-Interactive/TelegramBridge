package ru.astrainteractive.messagebridge.link.telegram.command

import org.telegram.telegrambots.meta.api.objects.User
import org.telegram.telegrambots.meta.api.objects.message.Message
import ru.astrainteractive.klibs.kstorage.api.CachedKrate
import ru.astrainteractive.klibs.kstorage.api.getValue
import ru.astrainteractive.messagebridge.core.api.util.toMessengerText
import ru.astrainteractive.messagebridge.link.api.api.LinkApi
import ru.astrainteractive.messagebridge.link.api.config.LinkTranslation
import ru.astrainteractive.messagebridge.link.api.mapping.asMessage
import ru.astrainteractive.messagebridge.link.api.model.LinkResponse
import ru.astrainteractive.messagebridge.link.api.player.model.LinkedPlayerModel
import ru.astrainteractive.messagebridge.messenger.telegram.api.api.TelegramMessageSender

internal class TelegramLinkHandler(
    private val linkApi: LinkApi,
    private val messageSender: TelegramMessageSender,
    translationKrate: CachedKrate<LinkTranslation>,
) {
    private val translation by translationKrate

    private suspend fun linkResponse(code: Int, user: User): LinkResponse {
        val username = user.userName ?: return LinkResponse.NoUsername
        val telegramLink = LinkedPlayerModel.TelegramLink(telegramUsername = username, telegramId = user.id)
        return linkApi.linkTelegram(code, telegramLink)
    }

    suspend fun link(code: Int, message: Message) {
        val user = message.from ?: return
        val text = linkResponse(code, user).asMessage(translation.link).toMessengerText()
        messageSender.send(message.chatId.toString(), text, replyToMessageId = message.replyToMessage?.messageId)
    }
}
