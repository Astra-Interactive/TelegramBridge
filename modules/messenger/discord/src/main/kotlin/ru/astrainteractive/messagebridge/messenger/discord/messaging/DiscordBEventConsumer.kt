package ru.astrainteractive.messagebridge.messenger.discord.messaging

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.retry
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import ru.astrainteractive.astralibs.coroutines.withTimings
import ru.astrainteractive.astralibs.localization.component.LocalizableComponent
import ru.astrainteractive.klibs.kstorage.api.CachedKrate
import ru.astrainteractive.klibs.kstorage.api.getValue
import ru.astrainteractive.klibs.mikro.core.coroutines.CoroutineFeature
import ru.astrainteractive.klibs.mikro.core.logging.JUtiltLogger
import ru.astrainteractive.klibs.mikro.core.logging.Logger
import ru.astrainteractive.messagebridge.core.PluginConfiguration
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
import ru.astrainteractive.messagebridge.messenger.discord.mapping.DiscordEmbedMapper
import ru.astrainteractive.messagebridge.messenger.discord.mapping.DiscordFailureMapper
import ru.astrainteractive.messagebridge.messenger.discord.mapping.DiscordWebhookMessageMapper
import ru.astrainteractive.messagebridge.messenger.discord.model.DiscordFailure
import ru.astrainteractive.messagebridge.messenger.discord.util.RestActionExt.await
import java.util.concurrent.atomic.AtomicReference
import kotlin.time.Duration.Companion.seconds

internal class DiscordBEventConsumer(
    private val channelProvider: DiscordChannelProvider,
    private val topicUpdater: DiscordTopicUpdater,
    private val embedMapper: DiscordEmbedMapper,
    private val memberResolver: DiscordMemberResolver,
    private val webhookMessageMapper: DiscordWebhookMessageMapper,
    private val failureMapper: DiscordFailureMapper,
    private val delivery: DiscordDeliveryError,
    configKrate: CachedKrate<PluginConfiguration>,
) : BEventConsumer,
    CoroutineFeature by CoroutineFeature.IO.withTimings(),
    Logger by JUtiltLogger("MessageBridge-DiscordBEventConsumer") {
    private val config by configKrate
    private val lastTopicFailure = AtomicReference<DiscordFailure?>(null)

    val deliveryError: StateFlow<LocalizableComponent?> = delivery.text

    override suspend fun consume(bEvent: BEvent) {
        if (bEvent.from == MessageFrom.DISCORD) return
        val jda = channelProvider.jda() ?: run {
            verbose { "#consume Discord is not connected, $bEvent is not sent" }
            return
        }
        val channel = channelProvider.textChannel(jda)
        when (bEvent) {
            is PlayerDeathBEvent -> channel.sendMessageEmbeds(embedMapper.map(bEvent)).await()
            is PlayerJoinedBEvent -> {
                updateTopic { topicUpdater.updateOnlineCount(channel) }
                channel.sendMessageEmbeds(embedMapper.map(bEvent)).await()
            }

            is PlayerLeaveBEvent -> {
                updateTopic { topicUpdater.updateOnlineCount(channel) }
                channel.sendMessageEmbeds(embedMapper.map(bEvent)).await()
            }

            is Text -> sendText(bEvent, channel)
            ServerClosedBEvent -> sendServerStatus(channel, SERVER_CLOSED_MESSAGE)
            ServerOpenBEvent -> sendServerStatus(channel, SERVER_OPEN_MESSAGE)
        }
        delivery.clear()
    }

    private suspend fun sendText(event: Text, channel: TextChannel) {
        val member = memberResolver.resolve(channel, event)
        val message = webhookMessageMapper.map(event, member)
        channelProvider.webHookClient().send(message)
    }

    private suspend fun sendServerStatus(channel: TextChannel, text: String) {
        updateTopic { topicUpdater.setStarting(channel) }
        channel.sendMessage(text).await()
    }

    /** The topic only shows the online count, so a failure to change it does not stop the message. */
    private suspend fun updateTopic(block: suspend () -> Unit) {
        try {
            block()
            lastTopicFailure.set(null)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val failure = failureMapper.map(e, config.jdaConfig)
            if (lastTopicFailure.getAndSet(failure) != failure) {
                warn { "#updateTopic could not change the topic of the channel: $failure" }
            }
        }
    }

    private suspend fun deliver(bEvent: BEvent) {
        flow { emit(consume(bEvent)) }
            .retry(DELIVERY_RETRIES) { throwable ->
                val isRetryable = failureMapper.map(throwable, config.jdaConfig).isRetryable
                if (isRetryable) delay(DELIVERY_RETRY_DELAY)
                isRetryable
            }
            .catch { throwable -> delivery.report(throwable) }
            .collect()
    }

    init {
        BEventChannel
            .bEvents(this)
            .onEach { verbose { "#init receive event $it" } }
            .onEach { bEvent -> deliver(bEvent) }
            .launchIn(this)
    }

    private companion object {
        const val SERVER_CLOSED_MESSAGE = "🛑 **Сервер остановлен**"
        const val SERVER_OPEN_MESSAGE = "✅ **Сервер успешно запущен**"
        const val DELIVERY_RETRIES = 3L
        val DELIVERY_RETRY_DELAY = 1.seconds
    }
}
