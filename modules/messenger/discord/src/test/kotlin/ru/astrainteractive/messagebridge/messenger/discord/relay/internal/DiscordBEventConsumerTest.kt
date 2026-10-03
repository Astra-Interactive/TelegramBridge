@file:Suppress("FunctionNaming")

package ru.astrainteractive.messagebridge.messenger.discord.relay.internal

import club.minnced.discord.webhook.WebhookClientBuilder
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction
import ru.astrainteractive.messagebridge.core.api.config.PluginConfiguration
import ru.astrainteractive.messagebridge.core.api.config.PluginTranslation
import ru.astrainteractive.messagebridge.core.api.fake.FakeClock
import ru.astrainteractive.messagebridge.core.api.fake.FakePlatformServer
import ru.astrainteractive.messagebridge.core.api.fake.FakeTranslationKrate
import ru.astrainteractive.messagebridge.link.api.player.fake.FakeLinkingDao
import ru.astrainteractive.messagebridge.messenger.api.impl.BEventChannel
import ru.astrainteractive.messagebridge.messenger.api.model.BEvent
import ru.astrainteractive.messagebridge.messenger.discord.channel.model.DiscordChannel
import ru.astrainteractive.messagebridge.messenger.discord.failure.internal.DiscordDeliveryError
import ru.astrainteractive.messagebridge.messenger.discord.failure.mapping.DiscordFailureMapper
import ru.astrainteractive.messagebridge.messenger.discord.fake.jdaFake
import ru.astrainteractive.messagebridge.messenger.discord.fake.jdaRequest
import java.io.IOException
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

class DiscordBEventConsumerTest {
    private val translationKrate = FakeTranslationKrate(PluginTranslation())
    private val configFlow = MutableStateFlow(PluginConfiguration())
    private val failureMapper = DiscordFailureMapper()
    private val delivery = DiscordDeliveryError(
        failureMapper = failureMapper,
        configFlow = configFlow,
        translationKrate = translationKrate
    )
    private val channel = MutableSharedFlow<DiscordChannel>(replay = 1)
    private val bEventChannel = BEventChannel()
    private val sentEmbeds = mutableListOf<MessageEmbed>()
    private var sendFailure: Throwable? = null
    private val textChannel = jdaFake<TextChannel> { method, args ->
        when (method.name) {
            "sendMessageEmbeds" -> {
                sentEmbeds += args.first() as MessageEmbed
                jdaRequest<MessageCreateAction>(failure = sendFailure)
            }

            else -> null
        }
    }
    private val webhookClient = WebhookClientBuilder(WEBHOOK_URL).build()
    private val steveJoined = BEvent.PlayerJoined(name = "Steve", uuid = STEVE_UUID, hasPlayedBefore = true)

    @AfterTest
    fun cleanup() {
        webhookClient.close()
    }

    private fun TestScope.startConsumer() {
        DiscordBEventConsumer(
            channel = channel,
            topicUpdater = DiscordTopicUpdater(
                platformServer = FakePlatformServer(emptyList()),
                clock = FakeClock(Instant.fromEpochSeconds(0)),
                translationKrate = translationKrate,
            ),
            embedMapper = DiscordEmbedMapper(translationKrate),
            memberResolver = DiscordMemberResolver(FakeLinkingDao()),
            webhookMessageMapper = DiscordWebhookMessageMapper(),
            failureMapper = failureMapper,
            delivery = delivery,
            configFlow = configFlow,
            scope = backgroundScope,
            translationKrate = translationKrate,
            bEventChannel = bEventChannel,
        ).start()
        runCurrent()
    }

    @Test
    fun GIVEN_ready_channel_WHEN_player_joins_THEN_the_embed_goes_to_the_channel() = runTest {
        channel.emit(DiscordChannel.Ready(textChannel, webhookClient))
        startConsumer()

        bEventChannel.consume(steveJoined)
        runCurrent()

        assertEquals(listOf("Steve joined the server"), sentEmbeds.map { embed -> embed.author?.name })
        assertNull(delivery.text.value)
    }

    @Test
    fun GIVEN_bot_without_token_WHEN_player_joins_THEN_nothing_is_sent_and_no_error_is_reported() = runTest {
        channel.emit(DiscordChannel.Disabled)
        startConsumer()

        bEventChannel.consume(steveJoined)
        runCurrent()

        assertEquals(emptyList<MessageEmbed>(), sentEmbeds)
        assertNull(delivery.text.value)
    }

    @Test
    fun GIVEN_channel_still_connecting_WHEN_it_becomes_ready_in_time_THEN_the_waiting_event_is_sent() = runTest {
        channel.emit(DiscordChannel.Connecting)
        startConsumer()
        bEventChannel.consume(steveJoined)
        advanceTimeBy(10.seconds)
        assertEquals(emptyList<MessageEmbed>(), sentEmbeds)

        channel.emit(DiscordChannel.Ready(textChannel, webhookClient))
        runCurrent()

        assertEquals(1, sentEmbeds.size)
    }

    @Test
    fun GIVEN_channel_that_stays_connecting_WHEN_the_wait_runs_out_THEN_the_event_is_dropped() = runTest {
        channel.emit(DiscordChannel.Connecting)
        startConsumer()
        bEventChannel.consume(steveJoined)
        advanceTimeBy(31.seconds)

        channel.emit(DiscordChannel.Ready(textChannel, webhookClient))
        runCurrent()

        assertEquals(emptyList<MessageEmbed>(), sentEmbeds)
    }

    @Test
    fun GIVEN_channel_that_rejects_the_message_WHEN_player_joins_THEN_it_is_retried_and_then_reported() = runTest {
        sendFailure = IOException("timeout")
        channel.emit(DiscordChannel.Ready(textChannel, webhookClient))
        startConsumer()

        bEventChannel.consume(steveJoined)
        advanceTimeBy(RETRIES_DURATION)
        runCurrent()

        assertEquals(ATTEMPTS, sentEmbeds.size)
        assertNotNull(delivery.text.value)
    }

    private companion object {
        const val STEVE_UUID = "069a79f4-44e9-4726-a5be-fca90e38aaf5"
        const val WEBHOOK_URL = "https://discord.com/api/webhooks/1/token"
        const val ATTEMPTS = 4
        val RETRIES_DURATION = 4.seconds
    }
}
