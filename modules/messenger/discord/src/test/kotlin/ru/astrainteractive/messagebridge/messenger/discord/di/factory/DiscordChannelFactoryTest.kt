@file:Suppress("FunctionNaming")
@file:OptIn(ExperimentalCoroutinesApi::class)

package ru.astrainteractive.messagebridge.messenger.discord.di.factory

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Webhook
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import net.dv8tion.jda.api.requests.RestAction
import okhttp3.OkHttpClient
import ru.astrainteractive.klibs.mikro.core.util.tryCast
import ru.astrainteractive.messagebridge.messenger.discord.fake.JdaAnswer
import ru.astrainteractive.messagebridge.messenger.discord.fake.jdaFake
import ru.astrainteractive.messagebridge.messenger.discord.model.DiscordChannel
import java.util.function.Consumer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class DiscordChannelFactoryTest {
    private val factory = DiscordChannelFactory(WebHookClientFactory())
    private val states = mutableListOf<DiscordChannel>()
    private val bridgeWebhook: Webhook = jdaFake(
        mapOf(
            "getName" to "BRIDGE_HOOK_$CHANNEL_ID",
            "getUrl" to "https://discord.com/api/webhooks/$WEBHOOK_ID/token"
        )
    )
    private val webhooksRetrieval: RestAction<List<Webhook>> = jdaFake(
        mapOf(
            "queue" to JdaAnswer { args ->
                args.first()
                    ?.tryCast<Consumer<List<Webhook>>>()
                    ?.accept(listOf(bridgeWebhook))
            }
        )
    )
    private val failedWebhooksRetrieval: RestAction<List<Webhook>> = jdaFake(
        mapOf(
            "queue" to JdaAnswer { args ->
                args[1]
                    ?.tryCast<Consumer<Throwable>>()
                    ?.accept(IllegalStateException("50013: Missing Permissions"))
            }
        )
    )
    private val bridgeChannel: TextChannel = jdaFake(mapOf("retrieveWebhooks" to webhooksRetrieval))
    private var textChannel: TextChannel? = bridgeChannel
    private var channelLookups = 0
    private val jda: JDA = jdaFake(
        mapOf(
            "awaitReady" to JdaAnswer { _ -> jda },
            "getTextChannelById" to JdaAnswer { _ ->
                channelLookups += 1
                textChannel
            },
            "getHttpClient" to OkHttpClient()
        )
    )

    private fun TestScope.collectChannel(scope: CoroutineScope): Job {
        val collection = scope.launch {
            factory.create(jda, CHANNEL_ID)
                .onEach { state -> states += state }
                .collect()
        }
        runCurrent()
        return collection
    }

    @Test
    fun GIVEN_channel_with_bridge_webhook_WHEN_created_THEN_it_is_ready_with_that_channel_and_webhook() = runTest {
        collectChannel(backgroundScope)

        val ready = assertIs<DiscordChannel.Ready>(states.single())
        assertSame(bridgeChannel, ready.textChannel)
        assertEquals(WEBHOOK_ID, ready.webhookClient.id)
    }

    @Test
    fun GIVEN_ready_channel_WHEN_its_collection_stops_THEN_the_webhook_client_is_closed() = runTest {
        val collection = collectChannel(this)
        val ready = assertIs<DiscordChannel.Ready>(states.single())
        assertFalse(ready.webhookClient.isShutdown)

        collection.cancel()
        runCurrent()

        assertTrue(ready.webhookClient.isShutdown)
    }

    @Test
    fun GIVEN_missing_channel_WHEN_created_THEN_it_is_failed_until_retried_five_seconds_later() = runTest {
        textChannel = null
        collectChannel(backgroundScope)

        assertEquals(listOf<DiscordChannel>(DiscordChannel.Failed), states)
        assertEquals(1, channelLookups)

        advanceTimeBy(RETRY_DELAY - 1.milliseconds)
        runCurrent()
        assertEquals(1, channelLookups)

        advanceTimeBy(1.milliseconds)
        runCurrent()
        assertEquals(2, channelLookups)
        assertEquals(listOf<DiscordChannel>(DiscordChannel.Failed, DiscordChannel.Failed), states)
    }

    @Test
    fun GIVEN_channel_that_appears_after_a_failure_WHEN_retried_THEN_it_becomes_ready() = runTest {
        textChannel = null
        collectChannel(backgroundScope)

        textChannel = bridgeChannel
        advanceTimeBy(RETRY_DELAY)
        runCurrent()

        assertEquals(DiscordChannel.Failed, states.first())
        assertSame(bridgeChannel, assertIs<DiscordChannel.Ready>(states.last()).textChannel)
        assertEquals(2, states.size)
    }

    @Test
    fun GIVEN_webhooks_that_cannot_be_retrieved_WHEN_created_THEN_it_is_failed() = runTest {
        textChannel = jdaFake(mapOf("retrieveWebhooks" to failedWebhooksRetrieval))
        collectChannel(backgroundScope)

        assertEquals(listOf<DiscordChannel>(DiscordChannel.Failed), states)
    }

    private companion object {
        const val CHANNEL_ID = "42"
        const val WEBHOOK_ID = 4242L
        val RETRY_DELAY = 5.seconds
    }
}
