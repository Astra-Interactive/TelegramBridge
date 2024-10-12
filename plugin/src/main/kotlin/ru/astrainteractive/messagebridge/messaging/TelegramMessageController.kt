package ru.astrainteractive.messagebridge.messaging

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.timeout
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import ru.astrainteractive.astralibs.logging.JUtiltLogger
import ru.astrainteractive.astralibs.logging.Logger
import ru.astrainteractive.klibs.kstorage.api.Krate
import ru.astrainteractive.messagebridge.core.PluginConfiguration
import ru.astrainteractive.messagebridge.core.PluginTranslation
import ru.astrainteractive.messagebridge.messaging.model.MessageEvent
import ru.astrainteractive.messagebridge.utils.getValue
import kotlin.time.Duration.Companion.seconds

class TelegramMessageController(
    configKrate: Krate<PluginConfiguration>,
    translationKrate: Krate<PluginTranslation>,
    private val telegramClientFlow: Flow<OkHttpTelegramClient>,
) : MessageController, Logger by JUtiltLogger("MessageBridge-TelegramMessageController") {
    private val config by configKrate
    private val translation by translationKrate

    private suspend fun telegramClientOrNull(): OkHttpTelegramClient? {
        return runCatching { telegramClientFlow.timeout(5.seconds).first() }
            .onFailure { error { "#onDisable could not get telegramClient: ${it.message} ${it.cause?.message}" } }
            .getOrNull()
    }

    override suspend fun send(messageEvent: MessageEvent) {
        if (messageEvent.from == MessageEvent.MessageFrom.TELEGRAM) return
        val text = when (messageEvent) {
            is MessageEvent.Text -> {
                translation.telegramMessageFormat(
                    playerName = messageEvent.author,
                    message = messageEvent.text,
                    from = messageEvent.from.short
                )
            }

            is MessageEvent.PlayerDeath -> {
                translation.playerDiedMessage(
                    name = messageEvent.name,
                    cause = messageEvent.cause
                )
            }

            is MessageEvent.PlayerJoined -> {
                translation.playerJoinMessage(
                    name = messageEvent.name,
                )
            }

            is MessageEvent.PlayerLeave -> {
                translation.playerLeaveMessage(
                    name = messageEvent.name,
                )
            }

            MessageEvent.ServerClosed -> {
                translation.serverClosedMessage
            }

            MessageEvent.ServerOpen -> {
                translation.serverOpenMessage
            }
        }.raw
        val sendMessage = SendMessage(config.chatID, text).apply {
            replyToMessageId = config.topicID.toIntOrNull()
        }
        telegramClientOrNull()?.execute(sendMessage)
    }
}
