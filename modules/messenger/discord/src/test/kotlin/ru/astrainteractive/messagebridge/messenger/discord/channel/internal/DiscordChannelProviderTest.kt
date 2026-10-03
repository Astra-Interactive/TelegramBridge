@file:Suppress("FunctionNaming")

package ru.astrainteractive.messagebridge.messenger.discord.channel.internal

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Webhook
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import net.dv8tion.jda.api.requests.RestAction
import net.dv8tion.jda.api.requests.restaction.WebhookAction
import okhttp3.OkHttpClient
import ru.astrainteractive.messagebridge.core.api.config.PluginConfiguration
import ru.astrainteractive.messagebridge.core.api.config.PluginTranslation
import ru.astrainteractive.messagebridge.core.api.fake.FakeTranslationKrate
import ru.astrainteractive.messagebridge.messenger.discord.channel.model.DiscordChannel
import ru.astrainteractive.messagebridge.messenger.discord.channel.network.WebHookClientFactory
import ru.astrainteractive.messagebridge.messenger.discord.failure.internal.DiscordDeliveryError
import ru.astrainteractive.messagebridge.messenger.discord.failure.mapping.DiscordFailureMapper
import ru.astrainteractive.messagebridge.messenger.discord.fake.jdaFake
import ru.astrainteractive.messagebridge.messenger.discord.fake.jdaRequest
import ru.astrainteractive.messagebridge.messenger.discord.model.DiscordConnection
import ru.astrainteractive.messagebridge.messenger.discord.model.DiscordFailure
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

class DiscordChannelProviderTest {
    private val connection = MutableStateFlow<DiscordConnection>(DiscordConnection.Connecting)
    private val configFlow = MutableStateFlow(configWithChannel(CHANNEL_ID))
    private val failureMapper = DiscordFailureMapper()
    private val deliveryError = DiscordDeliveryError(
        failureMapper = failureMapper,
        configFlow = configFlow,
        translationKrate = FakeTranslationKrate(PluginTranslation())
    )
    private val channels = mutableMapOf<Long, TextChannel>()
    private val webhooks = mutableListOf<Webhook>()
    private var webhooksFailure: Throwable? = null
    private val createdWebhooks = mutableListOf<String>()
    private val jda = jdaFake<JDA> { method, args ->
        when (method.name) {
            "getTextChannelById" -> channels[args.first() as Long]
            "getHttpClient" -> OkHttpClient()
            else -> null
        }
    }

    private fun configWithChannel(channelId: String): PluginConfiguration {
        val config = PluginConfiguration()
        return config.copy(jdaConfig = config.jdaConfig.copy(channelId = channelId))
    }

    private fun webhook(name: String): Webhook = jdaFake { method, _ ->
        when (method.name) {
            "getName" -> name
            "getUrl" -> WEBHOOK_URL
            else -> null
        }
    }

    private fun textChannel(id: String): TextChannel = jdaFake { method, args ->
        when (method.name) {
            "getId" -> id
            "getJDA" -> jda
            "retrieveWebhooks" -> jdaRequest<RestAction<List<Webhook>>>(
                value = webhooks.toList(),
                failure = webhooksFailure
            )

            "createWebhook" -> {
                val name = args.first() as String
                createdWebhooks += name
                jdaRequest<WebhookAction>(value = webhook(name))
            }

            else -> null
        }
    }

    private fun addChannel(id: String): TextChannel {
        return textChannel(id).also { channel -> channels[id.toLong()] = channel }
    }

    private fun TestScope.observe(): List<DiscordChannel> {
        val provider = DiscordChannelProvider(
            webhookClientFactory = WebHookClientFactory(),
            failureMapper = failureMapper,
            deliveryError = deliveryError,
            connection = connection,
            configFlow = configFlow,
            scope = backgroundScope,
        )
        val states = mutableListOf<DiscordChannel>()
        backgroundScope.launch { provider.channel.toList(states) }
        runCurrent()
        return states
    }

    @Test
    fun GIVEN_bot_without_token_WHEN_observed_THEN_the_channel_is_disabled() = runTest {
        connection.value = DiscordConnection.Disabled

        val states = observe()

        assertEquals(DiscordChannel.Disabled, states.last())
    }

    @Test
    fun GIVEN_connected_bot_without_channel_id_WHEN_observed_THEN_the_channel_is_not_set_and_nothing_is_reported() =
        runTest {
            configFlow.value = configWithChannel("")
            connection.value = DiscordConnection.Connected(jda)

            val states = observe()

            assertEquals(DiscordChannel.Failed(DiscordFailure.ChannelNotSet), states.last())
            assertNull(deliveryError.text.value)
        }

    @Test
    fun GIVEN_channel_with_bridge_webhook_WHEN_bot_connects_THEN_the_channel_is_ready_with_that_webhook() = runTest {
        val textChannel = addChannel(CHANNEL_ID)
        webhooks += webhook("BRIDGE_HOOK_$CHANNEL_ID")
        val states = observe()

        connection.value = DiscordConnection.Connected(jda)
        runCurrent()

        val ready = assertIs<DiscordChannel.Ready>(states.last())
        assertSame(textChannel, ready.textChannel)
        assertEquals(emptyList<String>(), createdWebhooks)
        assertNull(deliveryError.text.value)
    }

    @Test
    fun GIVEN_channel_without_bridge_webhook_WHEN_bot_connects_THEN_a_webhook_is_created() = runTest {
        addChannel(CHANNEL_ID)
        val states = observe()

        connection.value = DiscordConnection.Connected(jda)
        runCurrent()

        assertIs<DiscordChannel.Ready>(states.last())
        assertEquals(listOf("BRIDGE_HOOK_$CHANNEL_ID"), createdWebhooks)
    }

    @Test
    fun GIVEN_channel_the_bot_cannot_see_WHEN_it_appears_THEN_the_channel_is_ready_after_the_retry_delay() = runTest {
        val states = observe()
        connection.value = DiscordConnection.Connected(jda)
        runCurrent()
        assertEquals(DiscordChannel.Failed(DiscordFailure.ChannelNotFound(CHANNEL_ID)), states.last())
        assertNotNull(deliveryError.text.value)

        addChannel(CHANNEL_ID)
        advanceTimeBy(RETRY_DELAY)
        runCurrent()

        assertIs<DiscordChannel.Ready>(states.last())
        assertNull(deliveryError.text.value)
    }

    @Test
    fun GIVEN_webhook_request_that_fails_WHEN_the_next_attempt_succeeds_THEN_the_channel_is_ready() = runTest {
        addChannel(CHANNEL_ID)
        webhooksFailure = IOException("timeout")
        val states = observe()
        connection.value = DiscordConnection.Connected(jda)
        runCurrent()
        val failed = assertIs<DiscordChannel.Failed>(states.last())
        assertIs<DiscordFailure.Network>(failed.failure)
        assertNotNull(deliveryError.text.value)

        webhooksFailure = null
        advanceTimeBy(RETRY_DELAY)
        runCurrent()

        assertIs<DiscordChannel.Ready>(states.last())
        assertNull(deliveryError.text.value)
    }

    @Test
    fun GIVEN_ready_channel_WHEN_channel_id_changes_THEN_the_old_webhook_client_is_closed_and_the_new_one_is_ready() =
        runTest {
            addChannel(CHANNEL_ID)
            val otherChannel = addChannel(OTHER_CHANNEL_ID)
            val states = observe()
            connection.value = DiscordConnection.Connected(jda)
            runCurrent()
            val first = assertIs<DiscordChannel.Ready>(states.last())

            configFlow.value = configWithChannel(OTHER_CHANNEL_ID)
            runCurrent()

            val second = assertIs<DiscordChannel.Ready>(states.last())
            assertSame(otherChannel, second.textChannel)
            assertTrue(first.webhookClient.isShutdown)
            assertFalse(second.webhookClient.isShutdown)
        }

    @Test
    fun GIVEN_ready_channel_WHEN_the_bot_fails_THEN_the_channel_fails_without_a_delivery_error() = runTest {
        addChannel(CHANNEL_ID)
        val states = observe()
        connection.value = DiscordConnection.Connected(jda)
        runCurrent()
        val ready = assertIs<DiscordChannel.Ready>(states.last())

        connection.value = DiscordConnection.Failed(DiscordFailure.InvalidToken)
        runCurrent()

        assertEquals(DiscordChannel.Failed(DiscordFailure.InvalidToken), states.last())
        assertTrue(ready.webhookClient.isShutdown)
        assertNull(deliveryError.text.value)
    }

    private companion object {
        const val CHANNEL_ID = "100"
        const val OTHER_CHANNEL_ID = "200"
        const val WEBHOOK_URL = "https://discord.com/api/webhooks/1/token"
        val RETRY_DELAY = 30.seconds
    }
}
