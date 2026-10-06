package ru.astrainteractive.messagebridge.messenger.telegram.internal

import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.telegram.telegrambots.meta.api.objects.MessageEntity
import ru.astrainteractive.astralibs.coroutines.withTimings
import ru.astrainteractive.astralibs.localization.component.LocalizableComponent
import ru.astrainteractive.klibs.kstorage.api.CachedKrate
import ru.astrainteractive.klibs.kstorage.api.getValue
import ru.astrainteractive.klibs.mikro.core.coroutines.CoroutineFeature
import ru.astrainteractive.klibs.mikro.core.logging.JUtiltLogger
import ru.astrainteractive.klibs.mikro.core.logging.Logger
import ru.astrainteractive.messagebridge.core.api.config.PluginConfiguration
import ru.astrainteractive.messagebridge.core.api.config.PluginTranslation
import ru.astrainteractive.messagebridge.core.api.mapping.toMessengerText
import ru.astrainteractive.messagebridge.core.api.util.ellipsize
import ru.astrainteractive.messagebridge.messenger.api.api.BEventConsumer
import ru.astrainteractive.messagebridge.messenger.api.api.BEventReceiver
import ru.astrainteractive.messagebridge.messenger.api.model.BEvent
import ru.astrainteractive.messagebridge.messenger.api.model.MessageFrom
import ru.astrainteractive.messagebridge.messenger.api.model.MessageRef
import ru.astrainteractive.messagebridge.messenger.api.model.PlayerDeathBEvent
import ru.astrainteractive.messagebridge.messenger.api.model.PlayerJoinedBEvent
import ru.astrainteractive.messagebridge.messenger.api.model.PlayerLeaveBEvent
import ru.astrainteractive.messagebridge.messenger.api.model.ServerClosedBEvent
import ru.astrainteractive.messagebridge.messenger.api.model.ServerOpenBEvent
import ru.astrainteractive.messagebridge.messenger.api.model.Text

internal class TelegramBEventConsumer(
    configKrate: CachedKrate<PluginConfiguration>,
    translationKrate: CachedKrate<PluginTranslation>,
    private val messageSender: TelegramMessageSender,
    private val relayedMessageCache: TelegramRelayedMessageCache,
    private val bEventReceiver: BEventReceiver,
) : BEventConsumer,
    CoroutineFeature by CoroutineFeature.IO.withTimings(),
    Logger by JUtiltLogger("MessageBridge-TelegramBEventConsumer") {
    private val config by configKrate
    private val tgConfig: PluginConfiguration.TelegramConfig
        get() = config.tgConfig
    private val translation by translationKrate

    private suspend fun replyTargetOf(reply: Text.Reply): Int? {
        val target = reply.target ?: return null
        val chatId = tgConfig.chatID.toLongOrNull() ?: return null
        if (target is MessageRef.Telegram) {
            return target.messageId.takeIf { target.chatId == chatId }
        }
        return relayedMessageCache.copyOf(target, chatId)
    }

    private fun quoteEntities(text: String, quote: String): List<MessageEntity> {
        val offset = text.indexOf(quote)
        if (offset < 0) return emptyList()
        val entity = MessageEntity.builder()
            .type(BLOCKQUOTE_ENTITY)
            .offset(offset)
            .length(quote.length)
            .build()
        return listOf(entity)
    }

    private suspend fun relayText(bEvent: Text) {
        val topicId = tgConfig.topicID.toIntOrNull()
        val reply = bEvent.reply
        val replyTarget = reply?.let { knownReply -> replyTargetOf(knownReply) }
        val sentMessage = if (reply == null || replyTarget != null) {
            val text = translation.chat
                .toTelegram(playerName = bEvent.author, message = bEvent.text, from = bEvent.from.short)
                .toMessengerText()
            messageSender.send(tgConfig.chatID, text, replyToMessageId = replyTarget, topicId = topicId)
        } else {
            val quote = translation.chat
                .replyQuote(reply.author, reply.text.ellipsize(tgConfig.replyPreviewLength))
                .toMessengerText()
            val text = translation.chat
                .toTelegramReply(
                    playerName = bEvent.author,
                    message = bEvent.text,
                    from = bEvent.from.short,
                    quote = quote
                )
                .toMessengerText()
            messageSender.send(tgConfig.chatID, text, topicId = topicId, entities = quoteEntities(text, quote))
        } ?: return
        relayedMessageCache.remember(sentMessage.chatId, sentMessage.messageId, bEvent)
    }

    private suspend fun announce(text: LocalizableComponent) {
        messageSender.send(tgConfig.chatID, text.toMessengerText(), topicId = tgConfig.topicID.toIntOrNull())
    }

    private suspend fun send(bEvent: BEvent) {
        if (bEvent.from == MessageFrom.TELEGRAM) return
        when (bEvent) {
            is Text -> relayText(bEvent)

            is PlayerDeathBEvent -> {
                announce(
                    translation.player.died(
                        name = bEvent.name,
                        cause = bEvent.cause
                    )
                )
            }

            is PlayerJoinedBEvent -> {
                if (bEvent.hasPlayedBefore) {
                    announce(
                        translation.player.joined(
                            name = bEvent.name,
                        )
                    )
                } else {
                    announce(
                        translation.player.joinedFirstTime(
                            name = bEvent.name,
                        )
                    )
                }
            }

            is PlayerLeaveBEvent -> {
                announce(
                    translation.player.left(
                        name = bEvent.name,
                    )
                )
            }

            ServerClosedBEvent -> {
                announce(translation.server.stopped)
            }

            ServerOpenBEvent -> {
                announce(translation.server.started)
            }
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
            .receiveAsFlow()
            .onEach { bEvent -> consume(bEvent) }
            .launchIn(this)
    }

    private companion object {
        const val BLOCKQUOTE_ENTITY = "blockquote"
    }
}
