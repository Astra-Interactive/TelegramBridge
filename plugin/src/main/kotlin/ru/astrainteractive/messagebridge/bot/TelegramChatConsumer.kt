package ru.astrainteractive.messagebridge.bot

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.timeout
import kotlinx.coroutines.launch
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage
import org.telegram.telegrambots.meta.api.objects.Update
import ru.astrainteractive.astralibs.logging.JUtiltLogger
import ru.astrainteractive.astralibs.logging.Logger
import ru.astrainteractive.klibs.kstorage.api.Krate
import ru.astrainteractive.klibs.mikro.core.dispatchers.KotlinDispatchers
import ru.astrainteractive.messagebridge.core.PluginConfiguration
import ru.astrainteractive.messagebridge.messaging.MinecraftMessageController
import ru.astrainteractive.messagebridge.messaging.model.Message
import ru.astrainteractive.messagebridge.utils.getValue
import kotlin.time.Duration.Companion.seconds

class TelegramChatConsumer(
    configKrate: Krate<PluginConfiguration>,
    private val telegramClientFlow: Flow<OkHttpTelegramClient>,
    private val minecraftMessageController: MinecraftMessageController,
    private val scope: CoroutineScope,
    private val dispatchers: KotlinDispatchers
) : LongPollingSingleThreadUpdateConsumer, Logger by JUtiltLogger("MessageBridge-TelegramChatConsumer") {
    private val config by configKrate

    private suspend fun telegramClientOrNull(): OkHttpTelegramClient? {
        return kotlin.runCatching {
            telegramClientFlow.timeout(5.seconds).first()
        }.onFailure { error { "#onDisable could not get telegramClient: ${it.message} ${it.cause?.message}" } }
            .getOrNull()
    }

    fun Update.name(): String? {
        message.senderChat?.let {
            return it.userName ?: "${it.firstName ?: "Анонимус"} ${it.lastName ?: ""}"
        }
        message.from?.let {
            return it.userName ?: "${it.firstName ?: "Анонимус"} ${it.lastName ?: ""}"
        }
        return null
    }

    override fun consume(update: Update) {
        update ?: return
        scope.launch(dispatchers.IO) { onCommand(update) }
        if (config.chatID != update?.message?.chatId?.toString()) {
            info { "#consume configChatId!=message ${config.chatID}!=${update?.message?.chatId?.toString()}" }
            return
        }
        val replyMessageId = update.message?.replyToMessage?.messageId?.toString()
        val messageThreadId = update.message?.messageThreadId?.toString()

        if (config.topicID != (messageThreadId ?: replyMessageId)) {
            return
        }
        scope.launch(dispatchers.IO) {
            val deleteMessage = DeleteMessage(
                update.message.chatId.toString(),
                update.message.messageId
            )
            val author = update?.name() ?: run {
                info { "#consume author name is null" }
                telegramClientOrNull()?.execute(deleteMessage)
                return@launch
            }
            val text = update.message.text
            if (text.isNullOrBlank()) {
                telegramClientOrNull()?.execute(deleteMessage)
                info { "#consume text is null" }
                return@launch
            }
            val minecraftMessage = Message.Text(
                author = author,
                text = text,
                from = Message.MessageFrom.TELEGRAM
            )
            minecraftMessageController.send(minecraftMessage)
        }
    }

    private suspend fun onCommand(update: Update) {
        val text = update.message.text ?: return
        when (text) {
            "/minfo" -> {
                val chatId = update.message.chatId.toString()
                val originalMessageId = update.message?.replyToMessage?.messageId
                val message = "chatID is $chatId; originalMessageId: $originalMessageId"
                val sendMessage = SendMessage(chatId, message).apply {
                    replyToMessageId = originalMessageId
                }
                telegramClientOrNull()?.execute(sendMessage)
            }
        }
    }
}
