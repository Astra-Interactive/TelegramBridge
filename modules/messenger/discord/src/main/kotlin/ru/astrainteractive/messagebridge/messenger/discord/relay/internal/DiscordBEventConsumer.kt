package ru.astrainteractive.messagebridge.messenger.discord.relay.internal

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.future.await
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import ru.astrainteractive.astralibs.localization.component.LocalizableComponent
import ru.astrainteractive.klibs.kstorage.api.CachedKrate
import ru.astrainteractive.klibs.kstorage.api.getValue
import ru.astrainteractive.klibs.mikro.core.coroutines.propagateCancellationException
import ru.astrainteractive.klibs.mikro.core.logging.JUtiltLogger
import ru.astrainteractive.klibs.mikro.core.logging.Logger
import ru.astrainteractive.messagebridge.core.api.config.PluginConfiguration
import ru.astrainteractive.messagebridge.core.api.config.PluginTranslation
import ru.astrainteractive.messagebridge.core.api.util.toMessengerText
import ru.astrainteractive.messagebridge.messenger.api.impl.BEventChannel
import ru.astrainteractive.messagebridge.messenger.api.model.BEvent
import ru.astrainteractive.messagebridge.messenger.api.model.MessageFrom
import ru.astrainteractive.messagebridge.messenger.discord.channel.internal.DiscordChannelProvider
import ru.astrainteractive.messagebridge.messenger.discord.failure.internal.DiscordDeliveryError
import ru.astrainteractive.messagebridge.messenger.discord.failure.mapping.DiscordFailureMapper
import ru.astrainteractive.messagebridge.messenger.discord.model.DiscordFailure
import ru.astrainteractive.messagebridge.messenger.discord.util.awaitRequest
import kotlin.time.Duration.Companion.seconds

internal class DiscordBEventConsumer(
    private val channelProvider: DiscordChannelProvider,
    private val topicUpdater: DiscordTopicUpdater,
    private val embedMapper: DiscordEmbedMapper,
    private val memberResolver: DiscordMemberResolver,
    private val webhookMessageMapper: DiscordWebhookMessageMapper,
    private val failureMapper: DiscordFailureMapper,
    private val delivery: DiscordDeliveryError,
    private val configFlow: StateFlow<PluginConfiguration>,
    private val scope: CoroutineScope,
    translationKrate: CachedKrate<PluginTranslation>,
    private val bEventChannel: BEventChannel,
) : Logger by JUtiltLogger("MessageBridge-DiscordBEventConsumer") {
    private val config: PluginConfiguration
        get() = configFlow.value
    private val translation by translationKrate
    private val lastTopicFailureMutex = Mutex()
    private var lastTopicFailure: DiscordFailure? = null

    private suspend fun reportTopic(result: Result<Unit>) {
        val failure = result.exceptionOrNull()?.let { t -> failureMapper.map(t, config.jdaConfig) }
        val isNewFailure = lastTopicFailureMutex.withLock {
            val isNewFailure = lastTopicFailure != failure
            lastTopicFailure = failure
            isNewFailure
        }
        if (isNewFailure && failure != null) {
            warn { "#reportTopic could not change the topic of the channel: $failure" }
        }
    }

    private suspend fun sendEmbed(channel: TextChannel, embed: MessageEmbed): Result<Unit> {
        return awaitRequest { channel.sendMessageEmbeds(embed) }.map { _ -> }
    }

    private suspend fun sendText(event: BEvent.Text, channel: TextChannel): Result<Unit> {
        val member = memberResolver.resolve(channel, event).getOrElse { t -> return Result.failure(t) }
        val client = channelProvider.webhookClient().getOrElse { t -> return Result.failure(t) }
        val message = webhookMessageMapper.map(event, member)
        return runCatching { client.send(message).await() }
            .propagateCancellationException()
            .map { _ -> }
    }

    private suspend fun sendMessage(channel: TextChannel, text: LocalizableComponent): Result<Unit> {
        return awaitRequest { channel.sendMessage(text.toMessengerText()) }.map { _ -> }
    }

    private suspend fun send(bEvent: BEvent, jda: JDA): Result<Unit> {
        val channel = channelProvider.textChannel(jda).getOrElse { t -> return Result.failure(t) }
        return when (bEvent) {
            is BEvent.PlayerDeath -> sendEmbed(channel, embedMapper.map(bEvent))
            is BEvent.PlayerJoined -> {
                reportTopic(topicUpdater.updateOnlineCount(channel))
                sendEmbed(channel, embedMapper.map(bEvent))
            }

            is BEvent.PlayerLeave -> {
                reportTopic(topicUpdater.updateOnlineCount(channel))
                sendEmbed(channel, embedMapper.map(bEvent))
            }

            is BEvent.Text -> sendText(bEvent, channel)
            BEvent.ServerClosed -> {
                reportTopic(topicUpdater.setStopped(channel))
                sendMessage(channel, translation.discord.chat.serverStopped)
            }

            BEvent.ServerOpen -> {
                reportTopic(topicUpdater.setStarting(channel))
                sendMessage(channel, translation.discord.chat.serverStarted)
            }
        }
    }

    private suspend fun deliver(bEvent: BEvent) {
        if (bEvent.from == MessageFrom.DISCORD) return
        val jda = channelProvider.jda()
        if (jda == null) {
            verbose { "#deliver Discord is not connected, $bEvent is not sent" }
            return
        }
        var retriesLeft = DELIVERY_RETRIES
        while (true) {
            val t = send(bEvent, jda).exceptionOrNull()
            if (t == null) {
                delivery.clear()
                return
            }
            if (retriesLeft == 0 || !failureMapper.map(t, config.jdaConfig).isRetryable) {
                delivery.report(t)
                return
            }
            retriesLeft--
            delay(DELIVERY_RETRY_DELAY)
        }
    }

    fun start() {
        bEventChannel
            .bEvents(scope)
            .onEach { bEvent -> verbose { "#start receive event $bEvent" } }
            .onEach { bEvent -> deliver(bEvent) }
            .launchIn(scope)
    }

    private companion object {
        const val DELIVERY_RETRIES = 3
        val DELIVERY_RETRY_DELAY = 1.seconds
    }
}
