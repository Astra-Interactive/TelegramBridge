@file:Suppress("FunctionNaming")
@file:OptIn(ExperimentalCoroutinesApi::class)

package ru.astrainteractive.messagebridge.messenger.discord.messaging

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import ru.astrainteractive.messagebridge.core.PluginConfiguration
import ru.astrainteractive.messagebridge.messenger.discord.fake.FakeWebhookClient
import ru.astrainteractive.messagebridge.messenger.discord.fake.jdaFake
import ru.astrainteractive.messagebridge.messenger.discord.model.DiscordChannel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class DiscordChannelProviderTest {
    private val configured = PluginConfiguration.JdaConfig(token = "token", channelId = "42")
    private val ready = DiscordChannel.Ready(
        textChannel = jdaFake(mapOf("toString" to "#bridge")),
        webhookClient = FakeWebhookClient()
    )
    private val connectedConfigs = mutableListOf<PluginConfiguration.JdaConfig>()
    private val closedConfigs = mutableListOf<PluginConfiguration.JdaConfig>()

    private fun openSession(config: PluginConfiguration.JdaConfig): Flow<DiscordChannel> = callbackFlow {
        send(ready)
        awaitClose { closedConfigs += config }
    }

    private fun TestScope.provider(
        jdaConfigFlow: Flow<PluginConfiguration.JdaConfig>,
        session: (PluginConfiguration.JdaConfig) -> Flow<DiscordChannel>
    ): DiscordChannelProvider {
        val provider = DiscordChannelProvider(
            jdaConfigFlow = jdaConfigFlow,
            connect = { config ->
                connectedConfigs += config
                session.invoke(config)
            },
            scope = backgroundScope
        )
        runCurrent()
        return provider
    }

    private fun DiscordChannelProvider.state(): DiscordChannel = channel.replayCache.single()

    @Test
    fun GIVEN_blank_token_WHEN_provider_starts_THEN_discord_is_disabled_and_never_connected() = runTest {
        val provider = provider(MutableStateFlow(configured.copy(token = " ")), ::openSession)

        assertEquals(DiscordChannel.Disabled, provider.state())
        assertTrue(connectedConfigs.isEmpty())
    }

    @Test
    fun GIVEN_blank_channel_id_WHEN_provider_starts_THEN_discord_is_disabled_and_never_connected() = runTest {
        val provider = provider(MutableStateFlow(configured.copy(channelId = "")), ::openSession)

        assertEquals(DiscordChannel.Disabled, provider.state())
        assertTrue(connectedConfigs.isEmpty())
    }

    @Test
    fun GIVEN_configured_discord_WHEN_connection_has_not_finished_THEN_it_is_connecting() = runTest {
        val provider = provider(MutableStateFlow(configured)) { _ -> flow { awaitCancellation() } }

        assertEquals(DiscordChannel.Connecting, provider.state())
    }

    @Test
    fun GIVEN_configured_discord_WHEN_connected_THEN_the_channel_is_ready() = runTest {
        val provider = provider(MutableStateFlow(configured), ::openSession)

        assertSame(ready, provider.state())
        assertEquals(listOf(configured), connectedConfigs)
    }

    @Test
    fun GIVEN_connection_fails_WHEN_provider_starts_THEN_it_is_failed_until_retried_five_seconds_later() = runTest {
        var attempts = 0
        val provider = provider(MutableStateFlow(configured)) { _ ->
            flow {
                attempts += 1
                throw IllegalStateException("Invalid token")
            }
        }

        assertEquals(DiscordChannel.Failed, provider.state())
        assertEquals(1, attempts)

        advanceTimeBy(RETRY_DELAY - 1.milliseconds)
        runCurrent()
        assertEquals(1, attempts)

        advanceTimeBy(1.milliseconds)
        runCurrent()
        assertEquals(2, attempts)
        assertEquals(DiscordChannel.Failed, provider.state())
    }

    @Test
    fun GIVEN_ready_discord_WHEN_token_changes_THEN_the_old_session_closes_and_a_new_one_opens() = runTest {
        val configs = MutableStateFlow(configured)
        val provider = provider(configs, ::openSession)
        val reloaded = configured.copy(token = "new-token")

        configs.value = reloaded
        runCurrent()

        assertEquals(listOf(configured, reloaded), connectedConfigs)
        assertEquals(listOf(configured), closedConfigs)
        assertSame(ready, provider.state())
    }

    @Test
    fun GIVEN_ready_discord_WHEN_an_equal_config_arrives_again_THEN_it_is_not_reconnected() = runTest {
        val configs = MutableSharedFlow<PluginConfiguration.JdaConfig>(replay = 1)
        configs.emit(configured)
        val provider = provider(configs, ::openSession)

        configs.emit(configured.copy())
        runCurrent()

        assertEquals(listOf(configured), connectedConfigs)
        assertTrue(closedConfigs.isEmpty())
        assertSame(ready, provider.state())
    }

    @Test
    fun GIVEN_ready_discord_WHEN_token_is_cleared_THEN_the_session_closes_and_discord_is_disabled() = runTest {
        val configs = MutableStateFlow(configured)
        val provider = provider(configs, ::openSession)

        configs.value = configured.copy(token = "")
        runCurrent()

        assertEquals(listOf(configured), closedConfigs)
        assertEquals(DiscordChannel.Disabled, provider.state())
    }

    private companion object {
        val RETRY_DELAY = 5.seconds
    }
}
