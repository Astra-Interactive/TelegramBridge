package ru.astrainteractive.messagebridge.messenger.telegram.events

import org.telegram.telegrambots.meta.api.objects.Update
import ru.astrainteractive.astralibs.server.bridge.PlatformServer
import ru.astrainteractive.astralibs.server.player.OnlineKPlayer
import ru.astrainteractive.klibs.kstorage.api.CachedKrate
import ru.astrainteractive.klibs.kstorage.api.getValue
import ru.astrainteractive.klibs.mikro.core.logging.JUtiltLogger
import ru.astrainteractive.klibs.mikro.core.logging.Logger
import ru.astrainteractive.messagebridge.core.config.PluginTranslation
import ru.astrainteractive.messagebridge.core.mapping.toMessengerText
import ru.astrainteractive.messagebridge.messenger.telegram.messaging.TelegramMessageSender
import ru.astrainteractive.messagebridge.messenger.telegram.model.TelegramCommand

internal class TelegramCommandHandler(
    private val messageSender: TelegramMessageSender,
    private val platformServer: PlatformServer,
    translationKrate: CachedKrate<PluginTranslation>,
) : Logger by JUtiltLogger("MessageBridge-TelegramCommandHandler").withoutParentHandlers() {
    private val translation by translationKrate

    suspend fun handle(command: TelegramCommand, update: Update) {
        val message = update.message ?: return
        val chatId = message.chatId.toString()
        val originalMessageId = message.replyToMessage?.messageId
        when (command) {
            TelegramCommand.Vanilla -> sendVanilla(chatId, originalMessageId)
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
        val players = platformServer
            .getOnlinePlayers()
            .map(OnlineKPlayer::name)
        val text = translation.onlinePlayers.message(
            count = players.size,
            players = players.joinToString(separator = ", "),
        ).toMessengerText()
        messageSender.send(chatId, text, originalMessageId)
    }

    private companion object {
        const val INFO_COMMAND = "/minfo"
    }
}
