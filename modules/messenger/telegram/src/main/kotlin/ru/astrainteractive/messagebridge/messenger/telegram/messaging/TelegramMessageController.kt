package ru.astrainteractive.messagebridge.messenger.telegram.messaging

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException
import ru.astrainteractive.astralibs.logging.JUtiltLogger
import ru.astrainteractive.astralibs.logging.Logger
import ru.astrainteractive.klibs.kstorage.api.Krate
import ru.astrainteractive.messagebridge.core.PluginConfiguration
import ru.astrainteractive.messagebridge.core.PluginTranslation
import ru.astrainteractive.messagebridge.core.util.getValue
import ru.astrainteractive.messagebridge.messaging.MessageController
import ru.astrainteractive.messagebridge.messaging.model.ServerEvent

class TelegramMessageController(
    configKrate: Krate<PluginConfiguration>,
    translationKrate: Krate<PluginTranslation>,
    private val telegramClientFlow: Flow<OkHttpTelegramClient>,
) : MessageController, Logger by JUtiltLogger("MessageBridge-TelegramMessageController") {
    private val config by configKrate
    private val tgConfig: PluginConfiguration.TelegramConfig
        get() = config.tgConfig
    private val translation by translationKrate

    private suspend fun telegramClientOrNull(): OkHttpTelegramClient? {
        return runCatching { telegramClientFlow.firstOrNull() }
            .onFailure { error(it) { "#onDisable could not get telegramClient: ${it.message} ${it.cause?.message}" } }
            .getOrNull()
    }

    override suspend fun send(serverEvent: ServerEvent) {
        if (serverEvent.from == ServerEvent.MessageFrom.TELEGRAM) return
        val text = when (serverEvent) {
            is ServerEvent.Text -> {
                translation.telegramMessageFormat(
                    playerName = serverEvent.author,
                    message = serverEvent.text,
                    from = serverEvent.from.short
                )
            }

            is ServerEvent.PlayerDeath -> {
                translation.playerDiedMessage(
                    name = serverEvent.name,
                    cause = serverEvent.cause
                )
            }

            is ServerEvent.PlayerJoined -> {
                if (serverEvent.hasPlayedBefore) {
                    translation.playerJoinMessage(
                        name = serverEvent.name,
                    )
                } else {
                    translation.playerJoinMessageFirstTime(
                        name = serverEvent.name,
                    )
                }
            }

            is ServerEvent.PlayerLeave -> {
                translation.playerLeaveMessage(
                    name = serverEvent.name,
                )
            }

            ServerEvent.ServerClosed -> {
                translation.serverClosedMessage
            }

            ServerEvent.ServerOpen -> {
                translation.serverOpenMessage
            }
        }.raw
        val sendMessage = SendMessage(tgConfig.chatID, text).apply {
            replyToMessageId = tgConfig.topicID.toIntOrNull()
        }
        try {
            telegramClientOrNull()?.execute(sendMessage)
        } catch (e: TelegramApiRequestException) {
            if (e.errorCode == 404) {
                error { "#sendMessage: Wrong token, chat or topic id" }
            } else {
                error(e) { "#sendMessage unknown exception" }
            }
        }
    }
}
