package ru.astrainteractive.messagebridge.messenger.telegram.events

import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.User
import ru.astrainteractive.klibs.kstorage.api.CachedKrate
import ru.astrainteractive.klibs.kstorage.api.getValue
import ru.astrainteractive.klibs.mikro.core.logging.JUtiltLogger
import ru.astrainteractive.klibs.mikro.core.logging.Logger
import ru.astrainteractive.messagebridge.core.PluginTranslation
import ru.astrainteractive.messagebridge.core.api.OnlinePlayersProvider
import ru.astrainteractive.messagebridge.link.api.LinkApi
import ru.astrainteractive.messagebridge.link.mapping.asMessage
import ru.astrainteractive.messagebridge.messenger.telegram.messaging.TelegramMessageSender
import ru.astrainteractive.messagebridge.messenger.telegram.model.TelegramCommand

internal class TelegramCommandHandler(
    private val messageSender: TelegramMessageSender,
    private val onlinePlayersProvider: OnlinePlayersProvider,
    private val linkApi: LinkApi,
    translationKrate: CachedKrate<PluginTranslation>,
) : Logger by JUtiltLogger("MessageBridge-TelegramCommandHandler").withoutParentHandlers() {
    private val translation by translationKrate

    suspend fun handle(command: TelegramCommand, update: Update) {
        val message = update.message ?: return
        val chatId = message.chatId.toString()
        val originalMessageId = message.replyToMessage?.messageId
        when (command) {
            TelegramCommand.Vanilla -> sendVanilla(chatId, originalMessageId)
            is TelegramCommand.Link -> sendLink(command.code, message.from, chatId, originalMessageId)
        }
    }

    /**
     * Logs the chat/topic identifiers when the diagnostic `/minfo` command is received.
     * Runs regardless of the configured chat so operators can discover a chat's id.
     */
    fun logChatInfo(update: Update) {
        val message = update.message ?: return
        if (message.text != INFO_COMMAND) return
        info {
            "#logChatInfo chatID is ${message.chatId}; originalMessageId: ${message.replyToMessage?.messageId}"
        }
    }

    private suspend fun sendVanilla(chatId: String, originalMessageId: Int?) {
        val players = onlinePlayersProvider.provide()
        val text = translation.onlinePlayersMessage(
            count = players.size,
            players = players.joinToString(separator = ", "),
        ).raw
        messageSender.send(chatId, text, originalMessageId)
    }

    private suspend fun sendLink(code: Int, user: User?, chatId: String, originalMessageId: Int?) {
        user ?: return
        val response = linkApi.linkTelegram(code, user)
        val text = response.asMessage(translation.link).raw
        messageSender.send(chatId, text, originalMessageId)
    }

    private companion object {
        const val INFO_COMMAND = "/minfo"
    }
}
