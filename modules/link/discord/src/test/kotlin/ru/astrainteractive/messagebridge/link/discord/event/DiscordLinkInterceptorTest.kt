@file:Suppress("FunctionNaming")

package ru.astrainteractive.messagebridge.link.discord.event

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import net.dv8tion.jda.api.entities.channel.ChannelType
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import ru.astrainteractive.messagebridge.core.api.config.PluginConfiguration
import ru.astrainteractive.messagebridge.core.api.fake.FakeTranslationKrate
import ru.astrainteractive.messagebridge.link.api.config.LinkTranslation
import ru.astrainteractive.messagebridge.link.api.fake.FakeLinkApi
import ru.astrainteractive.messagebridge.link.api.model.LinkResponse
import ru.astrainteractive.messagebridge.link.discord.command.DiscordLinkHandler
import ru.astrainteractive.messagebridge.link.discord.fake.BRIDGE_CHANNEL_ID
import ru.astrainteractive.messagebridge.link.discord.fake.messageEventOf
import ru.astrainteractive.messagebridge.link.discord.internal.DiscordLinkRole
import ru.astrainteractive.messagebridge.messenger.discord.api.fake.RecordingDiscordMessageSender
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DiscordLinkInterceptorTest {
    private val linkApi = FakeLinkApi(response = LinkResponse.NoCode)
    private val config = PluginConfiguration(
        jdaConfig = PluginConfiguration.JdaConfig(channelId = "$BRIDGE_CHANNEL_ID")
    )

    private fun directMessageOf(text: String): MessageReceivedEvent {
        return messageEventOf(text = text, channelId = OTHER_CHANNEL_ID, channelType = ChannelType.PRIVATE)
    }

    private fun TestScope.intercept(event: MessageReceivedEvent): Boolean {
        val configFlow = MutableStateFlow(config)
        val interceptor = DiscordLinkInterceptor(
            scope = this,
            configFlow = configFlow,
            linkHandler = DiscordLinkHandler(
                linkApi = linkApi,
                linkRole = DiscordLinkRole(),
                messageSender = RecordingDiscordMessageSender(),
                configFlow = configFlow,
                translationKrate = FakeTranslationKrate(LinkTranslation())
            )
        )
        val isTaken = interceptor.intercept(event)
        advanceUntilIdle()
        return isTaken
    }

    @Test
    fun GIVEN_link_code_in_the_bridge_channel_WHEN_intercepted_THEN_the_code_is_linked() = runTest {
        assertTrue(intercept(messageEventOf(text = "/link 1234")))

        assertEquals(listOf(1234), linkApi.discordCodes)
    }

    @Test
    fun GIVEN_link_without_a_number_in_the_bridge_channel_WHEN_intercepted_THEN_an_invalid_code_is_tried() = runTest {
        assertTrue(intercept(messageEventOf(text = "/link abc")))

        assertEquals(listOf(-1), linkApi.discordCodes)
    }

    @Test
    fun GIVEN_code_in_a_direct_message_WHEN_intercepted_THEN_the_code_is_linked() = runTest {
        assertTrue(intercept(directMessageOf(text = "1234")))

        assertEquals(listOf(1234), linkApi.discordCodes)
    }

    @Test
    fun GIVEN_text_in_a_direct_message_WHEN_intercepted_THEN_an_invalid_code_is_tried() = runTest {
        assertTrue(intercept(directMessageOf(text = "hi")))

        assertEquals(listOf(-1), linkApi.discordCodes)
    }

    @Test
    fun GIVEN_link_code_in_another_channel_WHEN_intercepted_THEN_it_is_left_to_the_relay() = runTest {
        assertFalse(intercept(messageEventOf(text = "/link 1234", channelId = OTHER_CHANNEL_ID)))

        assertTrue(linkApi.discordCodes.isEmpty())
    }

    @Test
    fun GIVEN_chat_message_in_the_bridge_channel_WHEN_intercepted_THEN_it_is_left_to_the_relay() = runTest {
        assertFalse(intercept(messageEventOf(text = "hello")))
    }

    @Test
    fun GIVEN_link_code_from_a_webhook_WHEN_intercepted_THEN_it_is_left_to_the_relay() = runTest {
        assertFalse(intercept(messageEventOf(text = "/link 1234", isWebhookMessage = true)))
    }

    @Test
    fun GIVEN_direct_message_from_a_bot_WHEN_intercepted_THEN_it_is_ignored() = runTest {
        assertFalse(intercept(messageEventOf(text = "1234", channelType = ChannelType.PRIVATE, isBot = true)))

        assertTrue(linkApi.discordCodes.isEmpty())
    }

    private companion object {
        const val OTHER_CHANNEL_ID = 11L
    }
}
