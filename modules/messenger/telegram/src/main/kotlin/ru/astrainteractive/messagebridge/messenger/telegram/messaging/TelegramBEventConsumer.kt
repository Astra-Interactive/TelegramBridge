package ru.astrainteractive.messagebridge.messenger.telegram.messaging

import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
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
import ru.astrainteractive.messagebridge.messaging.model.BEvent
import ru.astrainteractive.messagebridge.messaging.model.BEvent.PlayerDeathBEvent
import ru.astrainteractive.messagebridge.messaging.model.BEvent.PlayerJoinedBEvent
import ru.astrainteractive.messagebridge.messaging.model.BEvent.PlayerLeaveBEvent
import ru.astrainteractive.messagebridge.messaging.model.BEvent.ServerClosedBEvent
import ru.astrainteractive.messagebridge.messaging.model.BEvent.ServerOpenBEvent
import ru.astrainteractive.messagebridge.messaging.model.BEvent.Text
import ru.astrainteractive.messagebridge.messaging.model.MessageFrom
import ru.astrainteractive.messagebridge.messenger.telegram.internal.TelegramRelayedMessageCache

internal class TelegramBEventConsumer(
    configKrate: CachedKrate<PluginConfiguration>,
    translationKrate: CachedKrate<PluginTranslation>,
    private val messageSender: TelegramMessageSender,
    private val relayedMessageCache: TelegramRelayedMessageCache,
    private val bEventReceiver: BEventReceiver,
) : BEventConsumer,
    CoroutineFeature by CoroutineFeature.IO.withTimings(),
    Logger by JUtiltLogger("MessageBridge-TelegramBEventConsumer").withoutParentHandlers() {
    private val config by configKrate
    private val tgConfig: PluginConfiguration.TelegramConfig
        get() = config.tgConfig
    private val translation by translationKrate

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
        val sentMessage = messageSender.send(tgConfig.chatID, text, tgConfig.topicID.toIntOrNull()) ?: return
        if (bEvent is Text) {
            relayedMessageCache.remember(sentMessage.chatId, sentMessage.messageId, bEvent)
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
            .onEach { bEvent -> consume(bEvent) }
            .launchIn(this)
    }
}
