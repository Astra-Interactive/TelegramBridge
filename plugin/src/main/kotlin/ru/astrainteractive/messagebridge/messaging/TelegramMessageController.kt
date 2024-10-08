package ru.astrainteractive.messagebridge.messaging

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.timeout
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import ru.astrainteractive.astralibs.logging.JUtiltLogger
import ru.astrainteractive.astralibs.logging.Logger
import ru.astrainteractive.klibs.kstorage.api.Krate
import ru.astrainteractive.messagebridge.bot.TelegramBot
import ru.astrainteractive.messagebridge.core.PluginConfiguration
import ru.astrainteractive.messagebridge.messaging.model.Message
import ru.astrainteractive.messagebridge.utils.getValue
import kotlin.time.Duration.Companion.seconds

class TelegramMessageController(
    configKrate: Krate<PluginConfiguration>,
    private val tgBotFlow: Flow<TelegramBot>
) : MessageController, Logger by JUtiltLogger("MessageBridge-TelegramMessageController") {
    private val config by configKrate

    override suspend fun send(message: Message) {
        val sendMessage = SendMessage().apply {
            this.chatId = config.channelID
            this.replyToMessageId = config.topicID.toIntOrNull()
        }
        when (message) {
            is Message.Text -> sendMessage.text = message.formattedText
        }
        runCatching { tgBotFlow.timeout(TIMEOUT).first().execute(sendMessage) }
            .onFailure { error { "#send could not send message: ${it.message} ${it.cause?.message}" } }
    }

    companion object {
        private val TIMEOUT = 5.seconds
    }
}
