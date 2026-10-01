package ru.astrainteractive.messagebridge.messenger.telegram.messaging

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.exceptions.TelegramApiException
import ru.astrainteractive.astralibs.coroutines.withTimings
import ru.astrainteractive.astralibs.localization.component.LocalizableComponent
import ru.astrainteractive.klibs.kstorage.api.CachedKrate
import ru.astrainteractive.klibs.kstorage.api.CachedMutableKrate
import ru.astrainteractive.klibs.kstorage.api.getValue
import ru.astrainteractive.klibs.mikro.core.coroutines.CoroutineFeature
import ru.astrainteractive.klibs.mikro.core.logging.JUtiltLogger
import ru.astrainteractive.klibs.mikro.core.logging.Logger
import ru.astrainteractive.messagebridge.core.PluginConfiguration
import ru.astrainteractive.messagebridge.core.PluginTranslation
import ru.astrainteractive.messagebridge.core.mapping.toMessengerText
import ru.astrainteractive.messagebridge.messaging.BEventConsumer
import ru.astrainteractive.messagebridge.messaging.internal.BEventChannel
import ru.astrainteractive.messagebridge.messaging.model.BEvent
import ru.astrainteractive.messagebridge.messaging.model.MessageFrom
import ru.astrainteractive.messagebridge.messaging.model.PlayerDeathBEvent
import ru.astrainteractive.messagebridge.messaging.model.PlayerJoinedBEvent
import ru.astrainteractive.messagebridge.messaging.model.PlayerLeaveBEvent
import ru.astrainteractive.messagebridge.messaging.model.ServerClosedBEvent
import ru.astrainteractive.messagebridge.messaging.model.ServerOpenBEvent
import ru.astrainteractive.messagebridge.messaging.model.Text
import ru.astrainteractive.messagebridge.messaging.tryConsume
import ru.astrainteractive.messagebridge.messenger.telegram.internal.TelegramRelayedMessageCache
import ru.astrainteractive.messagebridge.messenger.telegram.mapping.TelegramFailureMapper
import ru.astrainteractive.messagebridge.messenger.telegram.mapping.TelegramFailureTextMapper
import ru.astrainteractive.messagebridge.messenger.telegram.model.TelegramFailure

internal class TelegramBEventConsumer(
    private val configKrate: CachedMutableKrate<PluginConfiguration>,
    translationKrate: CachedKrate<PluginTranslation>,
    private val telegramClientFlow: Flow<OkHttpTelegramClient?>,
    private val failureMapper: TelegramFailureMapper,
    private val failureTextMapper: TelegramFailureTextMapper,
    private val relayedMessageCache: TelegramRelayedMessageCache,
) : BEventConsumer,
    CoroutineFeature by CoroutineFeature.IO.withTimings(),
    Logger by JUtiltLogger("MessageBridge-TelegramBEventConsumer") {
    private val config by configKrate
    private val tgConfig: PluginConfiguration.TelegramConfig
        get() = config.tgConfig
    private val translation by translationKrate

    private val _deliveryError = MutableStateFlow<LocalizableComponent?>(null)
    val deliveryError: StateFlow<LocalizableComponent?> = _deliveryError.asStateFlow()

    @Volatile
    private var lastFailure: TelegramFailure? = null

    private suspend fun telegramClientOrNull(): OkHttpTelegramClient? {
        return runCatching { telegramClientFlow.firstOrNull() }
            .onFailure { error(it) { "#onDisable could not get telegramClient: ${it.message} ${it.cause?.message}" } }
            .getOrNull()
    }

    override suspend fun consume(bEvent: BEvent) {
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
        val client = telegramClientOrNull() ?: return
        if (tgConfig.chatID.isBlank()) {
            onFailed(TelegramFailure.ChatNotSet)
            return
        }
        send(client, bEvent, text, isMigrationHandled = false)
    }

    private suspend fun send(client: OkHttpTelegramClient, bEvent: BEvent, text: String, isMigrationHandled: Boolean) {
        val sendMessage = SendMessage(tgConfig.chatID, text).apply {
            replyToMessageId = tgConfig.topicID.toIntOrNull()
        }
        try {
            val sentMessage = client.execute(sendMessage)
            if (bEvent is Text) {
                relayedMessageCache.remember(sentMessage.chatId, sentMessage.messageId, bEvent)
            }
            onDelivered()
        } catch (e: TelegramApiException) {
            val failure = failureMapper.map(e)
            if (failure is TelegramFailure.ChatMigrated && !isMigrationHandled) {
                migrate(failure.newChatId)
                send(client, bEvent, text, isMigrationHandled = true)
            } else {
                onFailed(failure)
            }
        }
    }

    private fun migrate(newChatId: Long) {
        configKrate.save { configuration ->
            configuration.copy(tgConfig = configuration.tgConfig.copy(chatID = "$newChatId"))
        }
        warn { translation.telegram.errors.chatIdChanged(newChatId).toMessengerText() }
    }

    private fun onDelivered() {
        if (lastFailure == null) return
        lastFailure = null
        _deliveryError.value = null
        info { "#send messages are delivered to Telegram again" }
    }

    private fun onFailed(failure: TelegramFailure) {
        val text = failureTextMapper.lazyMap(failure)
        if (failure == lastFailure) {
            verbose { "#send ${text.toMessengerText()}" }
            return
        }
        lastFailure = failure
        _deliveryError.value = text
        error { "#send could not send the message: ${text.toMessengerText()}" }
    }

    init {
        BEventChannel
            .bEvents(this)
            .onEach { bEvent -> tryConsume(bEvent) }
            .launchIn(this)
    }
}
