package ru.astrainteractive.messagebridge.messenger.telegram.impl.command.internal

import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.message.Message
import ru.astrainteractive.klibs.kstorage.api.CachedKrate
import ru.astrainteractive.klibs.kstorage.api.getValue
import ru.astrainteractive.messagebridge.core.api.api.OnlinePlayersProvider
import ru.astrainteractive.messagebridge.core.api.config.PluginTranslation
import ru.astrainteractive.messagebridge.core.api.util.toMessengerText
import ru.astrainteractive.messagebridge.link.api.LinkApi
import ru.astrainteractive.messagebridge.link.mapping.asMessage
import ru.astrainteractive.messagebridge.messenger.telegram.api.api.TelegramMessageSender
import ru.astrainteractive.messagebridge.messenger.telegram.impl.command.model.TelegramCommand

internal class TelegramCommandHandler(
    private val messageSender: TelegramMessageSender,
    private val onlinePlayersProvider: OnlinePlayersProvider,
    private val linkApi: LinkApi,
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
        }
    }
}
