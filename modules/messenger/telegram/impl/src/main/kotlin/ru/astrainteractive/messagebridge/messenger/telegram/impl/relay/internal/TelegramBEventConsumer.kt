package ru.astrainteractive.messagebridge.messenger.telegram.impl.relay.internal

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.message.Message
import ru.astrainteractive.astralibs.localization.component.LocalizableComponent
import ru.astrainteractive.klibs.kstorage.api.CachedKrate
import ru.astrainteractive.klibs.kstorage.api.MutableKrate
import ru.astrainteractive.klibs.kstorage.api.getValue
import ru.astrainteractive.klibs.mikro.core.logging.Logger
import ru.astrainteractive.messagebridge.core.api.config.PluginConfiguration
import ru.astrainteractive.messagebridge.core.api.config.PluginTranslation
import ru.astrainteractive.messagebridge.core.api.mapping.toMessengerText
import ru.astrainteractive.messagebridge.messenger.api.api.BEventConsumer
import ru.astrainteractive.messagebridge.messenger.api.model.BEvent
import ru.astrainteractive.messagebridge.messenger.api.model.MessageFrom
import ru.astrainteractive.messagebridge.messenger.api.model.PlayerDeathBEvent
import ru.astrainteractive.messagebridge.messenger.api.model.PlayerJoinedBEvent
import ru.astrainteractive.messagebridge.messenger.api.model.PlayerLeaveBEvent
import ru.astrainteractive.messagebridge.messenger.api.model.ServerClosedBEvent
import ru.astrainteractive.messagebridge.messenger.api.model.ServerOpenBEvent
import ru.astrainteractive.messagebridge.messenger.api.model.Text
import ru.astrainteractive.messagebridge.messenger.telegram.api.TelegramBotApi
import ru.astrainteractive.messagebridge.messenger.telegram.api.TelegramFailure
import ru.astrainteractive.messagebridge.messenger.telegram.api.TelegramFailureTextMapper
import ru.astrainteractive.messagebridge.messenger.telegram.api.TelegramRequestResult

internal class TelegramBEventConsumer(
    private val configFlow: StateFlow<PluginConfiguration>,
    private val configKrate: MutableKrate<Result<PluginConfiguration>>,
    translationKrate: CachedKrate<PluginTranslation>,
    private val botApi: TelegramBotApi,
    private val failureTextMapper: TelegramFailureTextMapper,
    private val relayedMessageCache: TelegramRelayedMessageCache,
    logger: Logger,
) : BEventConsumer,
    Logger by logger {
    private val tgConfig: PluginConfiguration.TelegramConfig
        get() = configFlow.value.tgConfig
    private val translation by translationKrate

    private val mutableDeliveryError = MutableStateFlow<LocalizableComponent?>(null)
    val deliveryError: StateFlow<LocalizableComponent?> = mutableDeliveryError.asStateFlow()

    @Volatile
    private var lastFailure: TelegramFailure? = null

    private fun textOf(bEvent: BEvent): LocalizableComponent = when (bEvent) {
        is Text -> translation.chat.toTelegram(
            playerName = bEvent.author,
            message = bEvent.text,
            from = bEvent.from.short
        )

        is PlayerDeathBEvent -> translation.player.died(name = bEvent.name, cause = bEvent.cause)

        is PlayerJoinedBEvent -> if (bEvent.hasPlayedBefore) {
            translation.player.joined(name = bEvent.name)
        } else {
            translation.player.joinedFirstTime(name = bEvent.name)
        }

        is PlayerLeaveBEvent -> translation.player.left(name = bEvent.name)

        ServerClosedBEvent -> translation.server.stopped

        ServerOpenBEvent -> translation.server.started
    }

    private fun onDelivered() {
        if (lastFailure == null) return
        lastFailure = null
        mutableDeliveryError.value = null
        info { translation.telegram.status.deliveryRestored.toMessengerText() }
    }

    private fun onFailed(failure: TelegramFailure) {
        val reason = failureTextMapper.lazyMap(failure)
        if (failure == lastFailure) {
            verbose { "#onFailed ${reason.toMessengerText()}" }
            return
        }
        lastFailure = failure
        mutableDeliveryError.value = reason
        error { translation.telegram.status.deliveryFailed(reason).toMessengerText() }
    }

    private suspend fun onResult(bEvent: BEvent, result: TelegramRequestResult<Message>) {
        when (result) {
            is TelegramRequestResult.Success -> {
                if (bEvent is Text) {
                    relayedMessageCache.remember(result.value.chatId, result.value.messageId, bEvent)
                }
                onDelivered()
            }

            TelegramRequestResult.NotConnected -> verbose { "#onResult the bot is not connected" }
            is TelegramRequestResult.Failed -> onFailed(result.failure)
        }
    }

    private fun saveMigratedChat(newChatId: Long): Boolean {
        val saved = configKrate.saveAndGet { result ->
            result.map { configuration ->
                configuration.copy(tgConfig = configuration.tgConfig.copy(chatID = "$newChatId"))
            }
        }
        saved.onSuccess { _ -> warn { translation.telegram.errors.chatIdChanged(newChatId).toMessengerText() } }
        return saved.isSuccess
    }

    private suspend fun send(chatId: String, text: String): TelegramRequestResult<Message> {
        val sendMessage = SendMessage(chatId, text).apply {
            replyToMessageId = tgConfig.topicID.toIntOrNull()
        }
        return botApi.execute(sendMessage)
    }

    private suspend fun deliver(bEvent: BEvent, text: String) {
        val result = send(tgConfig.chatID, text)
        val failure = (result as? TelegramRequestResult.Failed)?.failure
        if (failure !is TelegramFailure.ChatMigrated || !saveMigratedChat(failure.newChatId)) {
            onResult(bEvent, result)
            return
        }
        onResult(bEvent, send("${failure.newChatId}", text))
    }

    override suspend fun consume(bEvent: BEvent) {
        if (bEvent.from == MessageFrom.TELEGRAM) return
        if (tgConfig.token.isBlank()) return
        if (tgConfig.chatID.isBlank()) {
            onFailed(TelegramFailure.ChatNotSet)
            return
        }
        deliver(bEvent, textOf(bEvent).toMessengerText())
    }
}
