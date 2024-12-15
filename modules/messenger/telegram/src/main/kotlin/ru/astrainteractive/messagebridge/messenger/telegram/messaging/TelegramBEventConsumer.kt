package ru.astrainteractive.messagebridge.messenger.telegram.messaging

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException
import ru.astrainteractive.astralibs.async.CoroutineFeature
import ru.astrainteractive.astralibs.logging.JUtiltLogger
import ru.astrainteractive.astralibs.logging.Logger
import ru.astrainteractive.klibs.kstorage.api.Krate
import ru.astrainteractive.klibs.mikro.core.dispatchers.KotlinDispatchers
import ru.astrainteractive.messagebridge.core.PluginConfiguration
import ru.astrainteractive.messagebridge.core.PluginTranslation
import ru.astrainteractive.messagebridge.core.util.getValue
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

internal class TelegramBEventConsumer(
    configKrate: Krate<PluginConfiguration>,
    translationKrate: Krate<PluginTranslation>,
    private val telegramClientFlow: Flow<OkHttpTelegramClient>,
    private val dispatchers: KotlinDispatchers,
) : BEventConsumer,
    CoroutineFeature by CoroutineFeature.Default(dispatchers.Main),
    Logger by JUtiltLogger("MessageBridge-TelegramMessageController") {
    private val config by configKrate
    private val tgConfig: PluginConfiguration.TelegramConfig
        get() = config.tgConfig
    private val translation by translationKrate

    private suspend fun telegramClientOrNull(): OkHttpTelegramClient? {
        return runCatching { telegramClientFlow.firstOrNull() }
            .onFailure { error(it) { "#onDisable could not get telegramClient: ${it.message} ${it.cause?.message}" } }
            .getOrNull()
    }

    override suspend fun consume(bEvent: BEvent) {
        if (bEvent.from == MessageFrom.TELEGRAM) return
        val text = when (bEvent) {
            is Text -> {
                translation.telegramMessageFormat(
                    playerName = bEvent.author,
                    message = bEvent.text,
                    from = bEvent.from.short
                )
            }

            is PlayerDeathBEvent -> {
                translation.playerDiedMessage(
                    name = bEvent.name,
                    cause = bEvent.cause
                )
            }

            is PlayerJoinedBEvent -> {
                if (bEvent.hasPlayedBefore) {
                    translation.playerJoinMessage(
                        name = bEvent.name,
                    )
                } else {
                    translation.playerJoinMessageFirstTime(
                        name = bEvent.name,
                    )
                }
            }

            is PlayerLeaveBEvent -> {
                translation.playerLeaveMessage(
                    name = bEvent.name,
                )
            }

            ServerClosedBEvent -> {
                translation.serverClosedMessage
            }

            ServerOpenBEvent -> {
                translation.serverOpenMessage
            }
        }.raw
        val sendMessage = SendMessage(tgConfig.chatID, text).apply {
            replyToMessageId = tgConfig.topicID.toIntOrNull()
        }
        try {
            telegramClientOrNull()?.execute(sendMessage)
        } catch (e: TelegramApiRequestException) {
            if (e.errorCode == 404) {
                error { "#sendMessage: Wrong token, chat or topic id" }
            } else {
                error(e) { "#sendMessage unknown exception" }
            }
        }
    }

    init {
        BEventChannel
            .bEvents
            .onEach { bEvent -> tryConsume(bEvent) }
            .launchIn(this)
    }
}
