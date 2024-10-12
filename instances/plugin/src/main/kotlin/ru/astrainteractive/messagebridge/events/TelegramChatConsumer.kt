package ru.astrainteractive.messagebridge.events

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.timeout
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage
import org.telegram.telegrambots.meta.api.objects.Update
import ru.astrainteractive.astralibs.logging.JUtiltLogger
import ru.astrainteractive.astralibs.logging.Logger
import ru.astrainteractive.discordbot.module.bridge.api.BridgeApi
import ru.astrainteractive.discordbot.module.bridge.model.data.ServerEventMessageData
import ru.astrainteractive.klibs.kstorage.api.Krate
import ru.astrainteractive.klibs.mikro.core.dispatchers.KotlinDispatchers
import ru.astrainteractive.messagebridge.core.PluginConfiguration
import ru.astrainteractive.messagebridge.core.util.getValue
import ru.astrainteractive.messagebridge.messaging.MinecraftMessageController
import ru.astrainteractive.messagebridge.messaging.model.ServerEvent
import kotlin.time.Duration.Companion.seconds

class TelegramChatConsumer(
    configKrate: Krate<PluginConfiguration>,
    private val telegramClientFlow: Flow<OkHttpTelegramClient>,
    private val clientBridgeApi: BridgeApi,
    private val minecraftMessageController: MinecraftMessageController,
    private val scope: CoroutineScope,
    private val dispatchers: KotlinDispatchers,
) : LongPollingSingleThreadUpdateConsumer, Logger by JUtiltLogger("MessageBridge-TelegramChatConsumer") {
    private val config by configKrate

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
        if (config.chatID != update?.message?.chatId?.toString()) {
            info { "#consume configChatId!=message ${config.chatID}!=${update?.message?.chatId?.toString()}" }
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

        if (config.topicID != (messageThreadId ?: replyMessageId)) {
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
            if (text.length > config.maxTelegramMessageLength) {
                telegramClientOrNull()?.execute(deleteMessage)
                info { "#consume detect message with max chars limit" }
                return@launch
            }
            if (onCommand(update)) return@launch
            val serverEvent = ServerEvent.Text.Telegram(
                author = author,
                text = text,
            )
            clientBridgeApi.broadcastEvent(ServerEventMessageData(serverEvent))
            minecraftMessageController.send(serverEvent)
        }
    }

    private suspend fun onCommand(update: Update): Boolean {
        val text = update.message.text ?: return false
        val chatId = update.message.chatId.toString()
        val originalMessageId = update.message?.replyToMessage?.messageId
        when (text) {
            "/minfo" -> {
                val message = "chatID is $chatId; originalMessageId: $originalMessageId"
                val sendMessage = SendMessage(chatId, message).apply {
                    replyToMessageId = originalMessageId
                }
                telegramClientOrNull()?.execute(sendMessage)
                return true
            }

            "/vanilla" -> {
                val players = Bukkit.getOnlinePlayers().map(Player::getDisplayName)
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
        }
        return false
    }
}
