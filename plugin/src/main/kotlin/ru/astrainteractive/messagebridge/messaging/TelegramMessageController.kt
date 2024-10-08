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
import ru.astrainteractive.messagebridge.messaging.model.Message
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

    override suspend fun send(message: Message) {
        val text = when (message) {
            is Message.Text -> {
                translation.telegramMessageFormat(
                    playerName = message.author,
                    message = message.text
                )
            }

            is Message.PlayerDeath -> {
                translation.playerDiedMessage(
                    name = message.name,
                    cause = message.cause
                )
            }

            is Message.PlayerJoined -> {
                translation.playerJoinMessage(
                    name = message.name,
                )
            }

            is Message.PlayerLeave -> {
                translation.playerLeaveMessage(
                    name = message.name,
                )
            }
        }.raw
        val sendMessage = SendMessage(config.chatID, text).apply {
            replyToMessageId = config.topicID.toIntOrNull()
        }
        telegramClientOrNull()?.execute(sendMessage)
    }
}
