package ru.astrainteractive.messagebridge.messenger.telegram.events

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.timeout
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage
import org.telegram.telegrambots.meta.api.objects.Update
import ru.astrainteractive.astralibs.logging.JUtiltLogger
import ru.astrainteractive.astralibs.logging.Logger
import ru.astrainteractive.klibs.kstorage.api.Krate
import ru.astrainteractive.klibs.mikro.core.dispatchers.KotlinDispatchers
import ru.astrainteractive.messagebridge.OnlinePlayersProvider
import ru.astrainteractive.messagebridge.core.PluginConfiguration
import ru.astrainteractive.messagebridge.core.PluginTranslation
import ru.astrainteractive.messagebridge.core.util.getValue
import ru.astrainteractive.messagebridge.link.api.LinkApi
import ru.astrainteractive.messagebridge.link.mapping.asMessage
import ru.astrainteractive.messagebridge.messaging.MessageController
import ru.astrainteractive.messagebridge.messaging.model.ServerEvent
import kotlin.time.Duration.Companion.seconds

@Suppress("LongParameterList")
internal class TelegramChatConsumer(
    configKrate: Krate<PluginConfiguration>,
    translationKrate: Krate<PluginTranslation>,
    private val telegramClientFlow: Flow<OkHttpTelegramClient>,
    private val minecraftMessageController: MessageController,
    private val discordMessageController: MessageController,
    private val scope: CoroutineScope,
    private val dispatchers: KotlinDispatchers,
    private val onlinePlayersProvider: OnlinePlayersProvider,
    private val linkApi: LinkApi
) : LongPollingSingleThreadUpdateConsumer, Logger by JUtiltLogger("MessageBridge-TelegramChatConsumer") {
    private val config by configKrate
    private val translation by translationKrate
    private val tgConfig: PluginConfiguration.TelegramConfig
        get() = config.tgConfig

    private fun String.toFixedName(): String {
        val name = this
            .trim()
            .replace("\n", "")
            .replace("\t", "")
        return if (name.length > 16) {
            name.substring(0, 16)
        } else {
            name
        }
    }

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
        onInfoCommand(update)
        if (tgConfig.chatID != update?.message?.chatId?.toString()) {
            info { "#consume configChatId!=message ${tgConfig.chatID}!=${update?.message?.chatId?.toString()}" }
            return
        }
        val replyMessageId = update.message?.replyToMessage?.messageId?.toString()
        val messageThreadId = update.message?.messageThreadId?.toString()
        val date = update.message?.date?.toLong() ?: run {
            info { "#consume the date of message is null" }
            return
        }

        val triggerTime = kotlinx.datetime.Instant.fromEpochSeconds(date)
        val now = Clock.System.now()
        val diff = now.minus(triggerTime)
        if (diff > 10.seconds) {
            info { "#consume message is too old: $triggerTime vs $now ->  $diff" }
            return
        }

        if (tgConfig.topicID != (messageThreadId ?: replyMessageId)) {
            return
        }
        scope.launch(dispatchers.IO) {
            val deleteMessage = DeleteMessage(
                update.message.chatId.toString(),
                update.message.messageId
            )
            val author = update.name()?.toFixedName() ?: run {
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
            if (text.length > tgConfig.maxTelegramMessageLength) {
                telegramClientOrNull()?.execute(deleteMessage)
                info { "#consume detect message with max chars limit" }
                return@launch
            }
            if (onCommand(update)) return@launch
            val serverEvent = ServerEvent.Text.Telegram(
                author = author,
                text = text,
            )
            minecraftMessageController.send(serverEvent)
            discordMessageController.send(serverEvent)
        }
    }

    private fun onInfoCommand(update: Update) {
        val text = update.message.text ?: return
        val chatId = update.message.chatId.toString()
        val originalMessageId = update.message?.replyToMessage?.messageId
        when {
            text == "/minfo" -> {
                val message = "chatID is $chatId; originalMessageId: $originalMessageId"
                info { "#onInfoCommand -> $message" }
            }
        }
    }

    private suspend fun onCommand(update: Update): Boolean {
        val text = update.message.text ?: return false
        val chatId = update.message.chatId.toString()
        val originalMessageId = update.message?.replyToMessage?.messageId
        when {
            text.equals("/vanilla") -> {
                val players = onlinePlayersProvider.provide()
                val message = players.joinToString(
                    ", ",
                    prefix = "Сейчас онлайн ${players.size} игроков\n"
                )
                val sendMessage = SendMessage(chatId, message).apply {
                    replyToMessageId = originalMessageId
                }
                telegramClientOrNull()?.execute(sendMessage)
                return true
            }

            text.startsWith("/link") -> {
                val code = text.replace("/link ", "").toIntOrNull() ?: -1
                val user = update.message?.from ?: return true
                val response = linkApi.linkTelegram(code, user)
                val message = response.asMessage(translation.link).raw

                val sendMessage = SendMessage(chatId, message).apply {
                    replyToMessageId = originalMessageId
                }
                telegramClientOrNull()?.execute(sendMessage)
                return true
            }
        }
        return false
    }
}
