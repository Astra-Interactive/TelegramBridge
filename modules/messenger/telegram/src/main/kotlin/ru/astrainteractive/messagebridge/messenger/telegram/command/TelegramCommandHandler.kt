package ru.astrainteractive.messagebridge.messenger.telegram.command

import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.message.Message
import ru.astrainteractive.klibs.kstorage.api.CachedKrate
import ru.astrainteractive.klibs.kstorage.api.getValue
import ru.astrainteractive.messagebridge.core.PluginTranslation
import ru.astrainteractive.messagebridge.core.api.OnlinePlayersProvider
import ru.astrainteractive.messagebridge.core.mapping.toMessengerText
import ru.astrainteractive.messagebridge.link.LinkApi
import ru.astrainteractive.messagebridge.link.asMessage
import ru.astrainteractive.messagebridge.messenger.telegram.onboarding.TelegramBindHandler
import ru.astrainteractive.messagebridge.messenger.telegram.onboarding.TelegramChatInfoHandler
import ru.astrainteractive.messagebridge.messenger.telegram.request.TelegramMessageSender

/** The answers go to the message the command replies to, so they stay in the topic or the reply thread. */
internal class TelegramCommandHandler(
    private val messageSender: TelegramMessageSender,
    private val onlinePlayersProvider: OnlinePlayersProvider,
    private val linkApi: LinkApi,
    private val bindHandler: TelegramBindHandler,
    private val chatInfoHandler: TelegramChatInfoHandler,
    translationKrate: CachedKrate<PluginTranslation>,
) {
    private val translation by translationKrate

    private suspend fun answer(message: Message, text: String) {
        messageSender.send(message.chatId.toString(), text, replyToMessageId = message.replyToMessage?.messageId)
    }

    private suspend fun sendVanilla(message: Message) {
        val players = onlinePlayersProvider.provide()
        val text = translation.onlinePlayers.message(
            count = players.size,
            players = players.joinToString(separator = ", "),
        )
        answer(message, text.toMessengerText())
    }

    private suspend fun sendLink(code: Int, message: Message) {
        val user = message.from ?: return
        val response = linkApi.linkTelegram(code, user)
        answer(message, response.asMessage(translation.link).toMessengerText())
    }

    suspend fun handle(command: TelegramCommand, update: Update) {
        val message = update.message ?: return
        when (command) {
            TelegramCommand.Vanilla -> sendVanilla(message)
            is TelegramCommand.Link -> sendLink(command.code, message)
            is TelegramCommand.Bind -> bindHandler.bind(command.code, message)
            TelegramCommand.ChatInfo -> chatInfoHandler.sendChatInfo(message)
        }
    }
}
