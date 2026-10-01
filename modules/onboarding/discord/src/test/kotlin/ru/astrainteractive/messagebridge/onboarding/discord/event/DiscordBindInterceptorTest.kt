@file:Suppress("FunctionNaming")

package ru.astrainteractive.messagebridge.onboarding.discord.event

import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.entities.channel.ChannelType
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import ru.astrainteractive.messagebridge.core.api.config.PluginConfiguration
import ru.astrainteractive.messagebridge.core.api.fake.FakeClock
import ru.astrainteractive.messagebridge.core.api.fake.FakeConfigKrate
import ru.astrainteractive.messagebridge.core.api.fake.FakeTranslationKrate
import ru.astrainteractive.messagebridge.core.api.util.toMessengerText
import ru.astrainteractive.messagebridge.messenger.discord.api.fake.RecordingDiscordMessageSender
import ru.astrainteractive.messagebridge.messenger.discord.api.fake.jdaFake
import ru.astrainteractive.messagebridge.onboarding.api.config.OnboardingTranslation
import ru.astrainteractive.messagebridge.onboarding.api.impl.BindCodes
import ru.astrainteractive.messagebridge.onboarding.discord.command.DiscordBindCommandParser
import ru.astrainteractive.messagebridge.onboarding.discord.command.DiscordBindHandler
import java.util.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

class DiscordBindInterceptorTest {
    private val translation = OnboardingTranslation()
    private val messageSender = RecordingDiscordMessageSender()

    private fun eventOf(
        text: String,
        channelType: ChannelType = ChannelType.TEXT,
        isWebhookMessage: Boolean = false,
        isBot: Boolean = false
    ): MessageReceivedEvent {
        val channel = jdaFake<MessageChannelUnion> { method, _ -> if (method.name == "getType") channelType else null }
        val author = jdaFake<User> { method, _ -> if (method.name == "isBot") isBot else null }
        val member = jdaFake<Member> { _, _ -> null }
        val message = jdaFake<Message> { method, _ ->
            when (method.name) {
                "getIdLong" -> 1L
                "getChannel" -> channel
                "getContentRaw" -> text
                "isWebhookMessage" -> isWebhookMessage
                "getAuthor" -> author
                "getMember" -> member
                else -> null
            }
        }
        return MessageReceivedEvent(jdaFake<JDA> { _, _ -> null }, 0L, message)
    }

    private fun TestScope.intercept(event: MessageReceivedEvent): Boolean {
        val interceptor = DiscordBindInterceptor(
            scope = this,
            commandParser = DiscordBindCommandParser(),
            bindHandler = DiscordBindHandler(
                bindCodes = BindCodes(
                    clock = FakeClock(now = Instant.fromEpochSeconds(NOW_EPOCH_SECONDS)),
                    lifetime = 10.minutes,
                    random = Random(1)
                ),
                configKrate = FakeConfigKrate(Result.success(PluginConfiguration())),
                messageSender = messageSender,
                translationKrate = FakeTranslationKrate(translation)
            )
        )
        val isTaken = interceptor.intercept(event)
        advanceUntilIdle()
        return isTaken
    }

    @Test
    fun GIVEN_bind_code_from_a_server_member_WHEN_intercepted_THEN_bind_handler_answers() = runTest {
        val isTaken = intercept(eventOf(text = "!bind 00000000"))

        assertTrue(isTaken)
        assertEquals(listOf(translation.discord.bind.codeInvalid.toMessengerText()), messageSender.replies)
    }

    @Test
    fun GIVEN_chat_message_WHEN_intercepted_THEN_it_is_left_to_the_relay() = runTest {
        assertFalse(intercept(eventOf(text = "hello")))
    }

    @Test
    fun GIVEN_bind_code_in_a_direct_message_WHEN_intercepted_THEN_it_is_left_to_the_relay() = runTest {
        assertFalse(intercept(eventOf(text = "!bind 00000000", channelType = ChannelType.PRIVATE)))
    }

    @Test
    fun GIVEN_bind_code_from_a_webhook_WHEN_intercepted_THEN_it_is_left_to_the_relay() = runTest {
        assertFalse(intercept(eventOf(text = "!bind 00000000", isWebhookMessage = true)))
    }

    @Test
    fun GIVEN_bind_code_from_a_bot_WHEN_intercepted_THEN_it_is_left_to_the_relay() = runTest {
        assertFalse(intercept(eventOf(text = "!bind 00000000", isBot = true)))
        assertTrue(messageSender.replies.isEmpty())
    }

    private companion object {
        const val NOW_EPOCH_SECONDS = 1_700_000_000L
    }
}
