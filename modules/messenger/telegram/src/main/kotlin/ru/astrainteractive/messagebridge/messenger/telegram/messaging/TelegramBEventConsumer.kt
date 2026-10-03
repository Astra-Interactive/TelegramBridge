package ru.astrainteractive.messagebridge.messenger.telegram.messaging

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.exceptions.TelegramApiException
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException
import ru.astrainteractive.astralibs.coroutines.withTimings
import ru.astrainteractive.klibs.kstorage.api.CachedKrate
import ru.astrainteractive.klibs.kstorage.api.getValue
import ru.astrainteractive.klibs.mikro.core.coroutines.CoroutineFeature
import ru.astrainteractive.klibs.mikro.core.logging.JUtiltLogger
import ru.astrainteractive.klibs.mikro.core.logging.Logger
import ru.astrainteractive.messagebridge.core.PluginConfiguration
import ru.astrainteractive.messagebridge.core.PluginTranslation
import ru.astrainteractive.messagebridge.core.mapping.toMessengerText
import ru.astrainteractive.messagebridge.messaging.api.BEventConsumer
import ru.astrainteractive.messagebridge.messaging.api.BEventReceiver
import ru.astrainteractive.messagebridge.messaging.api.tryConsume
import ru.astrainteractive.messagebridge.messaging.model.BEvent
import ru.astrainteractive.messagebridge.messaging.model.MessageFrom
import ru.astrainteractive.messagebridge.messaging.model.PlayerDeathBEvent
import ru.astrainteractive.messagebridge.messaging.model.PlayerJoinedBEvent
import ru.astrainteractive.messagebridge.messaging.model.PlayerLeaveBEvent
import ru.astrainteractive.messagebridge.messaging.model.ServerClosedBEvent
import ru.astrainteractive.messagebridge.messaging.model.ServerOpenBEvent
import ru.astrainteractive.messagebridge.messaging.model.Text
import ru.astrainteractive.messagebridge.messenger.telegram.internal.TelegramRelayedMessageCache

internal class TelegramBEventConsumer(
    configKrate: CachedKrate<PluginConfiguration>,
    translationKrate: CachedKrate<PluginTranslation>,
    private val telegramClientFlow: Flow<OkHttpTelegramClient>,
    private val relayedMessageCache: TelegramRelayedMessageCache,
    private val bEventReceiver: BEventReceiver,
) : BEventConsumer,
    CoroutineFeature by CoroutineFeature.IO.withTimings(),
    Logger by JUtiltLogger("MessageBridge-TelegramBEventConsumer").withoutParentHandlers() {
    private val config by configKrate
    private val tgConfig: PluginConfiguration.TelegramConfig
        get() = config.tgConfig
    private val translation by translationKrate

    private suspend fun telegramClientOrNull(): OkHttpTelegramClient? {
        return runCatching { telegramClientFlow.firstOrNull() }
            .onFailure { t -> error(t) { "#onDisable could not get telegramClient: ${t.message} ${t.cause?.message}" } }
            .getOrNull()
    }

    private suspend fun send(bEvent: BEvent) {
        if (bEvent.from == MessageFrom.TELEGRAM) return
        val text = when (bEvent) {
            is Text -> {
                translation.chat.toTelegram(
                    playerName = bEvent.author,
                    message = bEvent.text,
                    from = bEvent.from.short
                )
            }

            is PlayerDeathBEvent -> {
                translation.player.died(
                    name = bEvent.name,
                    cause = bEvent.cause
                )
            }

            is PlayerJoinedBEvent -> {
                if (bEvent.hasPlayedBefore) {
                    translation.player.joined(
                        name = bEvent.name,
                    )
                } else {
                    translation.player.joinedFirstTime(
                        name = bEvent.name,
                    )
                }
            }

            is PlayerLeaveBEvent -> {
                translation.player.left(
                    name = bEvent.name,
                )
            }

            ServerClosedBEvent -> {
                translation.server.stopped
            }

            ServerOpenBEvent -> {
                translation.server.started
            }
        }.toMessengerText()
        val sendMessage = SendMessage(tgConfig.chatID, text).apply {
            replyToMessageId = tgConfig.topicID.toIntOrNull()
        }
        try {
            val sentMessage = telegramClientOrNull()?.execute(sendMessage)
            if (sentMessage != null && bEvent is Text) {
                relayedMessageCache.remember(sentMessage.chatId, sentMessage.messageId, bEvent)
            }
        } catch (e: TelegramApiRequestException) {
            @Suppress("MagicNumber")
            if (e.errorCode == 404) {
                error { "#sendMessage: Wrong token, chat or topic id" }
            } else {
                error(e) { "#sendMessage unknown exception" }
            }
        } catch (e: TelegramApiException) {
            error { "#sendMessage: Got TelegramApiException: ${e.message}. Probably fake exception." }
        }
    }

    override suspend fun consume(bEvent: BEvent) {
        if (tgConfig.token.isBlank() || tgConfig.chatID.isBlank()) {
            verbose { "#consume Telegram is not configured, skipped $bEvent" }
            return
        }
        send(bEvent)
    }

    init {
        bEventReceiver
            .bEvents(this)
            .onEach { bEvent -> tryConsume(bEvent) }
            .launchIn(this)
    }
}
