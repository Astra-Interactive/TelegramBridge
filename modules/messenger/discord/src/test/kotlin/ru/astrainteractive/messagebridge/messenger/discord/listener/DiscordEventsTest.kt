@file:Suppress("FunctionNaming", "ArgumentPerLine", "ExpressionBodyOnOneLine")
@file:OptIn(ExperimentalCoroutinesApi::class)

package ru.astrainteractive.messagebridge.messenger.discord.listener

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.job
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.entities.channel.ChannelType
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.InterfacedEventManager
import ru.astrainteractive.klibs.kstorage.api.asCachedKrate
import ru.astrainteractive.klibs.kstorage.api.impl.DefaultMutableKrate
import ru.astrainteractive.messagebridge.core.api.config.model.PluginConfiguration
import ru.astrainteractive.messagebridge.core.api.config.model.PluginTranslation
import ru.astrainteractive.messagebridge.messenger.api.api.MessageInterceptor
import ru.astrainteractive.messagebridge.messenger.api.discord.model.DiscordMemberLeave
import ru.astrainteractive.messagebridge.messenger.api.discord.model.DiscordUserId
import ru.astrainteractive.messagebridge.messenger.api.fake.FakeBEventConsumer
import ru.astrainteractive.messagebridge.messenger.api.model.Interception
import ru.astrainteractive.messagebridge.messenger.discord.fake.jdaFake
import ru.astrainteractive.messagebridge.messenger.discord.message.DiscordCommandHandler
import ru.astrainteractive.messagebridge.messenger.discord.message.DiscordMessageSender
import ru.astrainteractive.messagebridge.messenger.discord.message.MessageEventListener
import ru.astrainteractive.messagebridge.messenger.discord.message.mapping.DiscordCommandMapper
import ru.astrainteractive.messagebridge.messenger.discord.message.mapping.DiscordMessageRelevanceMapper
import ru.astrainteractive.messagebridge.messenger.discord.message.mapping.DiscordReplyMapper
import ru.astrainteractive.messagebridge.messenger.discord.relay.DiscordRelayedMessageCache
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DiscordEventsTest {
    private val eventManager = InterfacedEventManager()
    private val interceptedContents = mutableListOf<String>()
    private val memberLeaves = Channel<DiscordMemberLeave>(Channel.UNLIMITED)
    private val messageEventListener = MessageEventListener(
        relevanceMapper = DiscordMessageRelevanceMapper(
            configKrate = DefaultMutableKrate(factory = { PluginConfiguration() }, loader = { null }).asCachedKrate()
        ),
        commandMapper = DiscordCommandMapper(),
        commandHandler = DiscordCommandHandler(
            messageSender = DiscordMessageSender(),
            platformServer = jdaFake(emptyMap()),
            translationKrate = DefaultMutableKrate(factory = { PluginTranslation() }, loader = { null }).asCachedKrate()
        ),
        replyMapper = DiscordReplyMapper(
            translationKrate = DefaultMutableKrate(factory = { PluginTranslation() }, loader = { null })
                .asCachedKrate(),
            relayedMessageCache = DiscordRelayedMessageCache(
                configKrate = DefaultMutableKrate(factory = { PluginConfiguration() }, loader = { null })
                    .asCachedKrate()
            )
        ),
        messageSender = DiscordMessageSender(),
        messageInterceptors = listOf(
            MessageInterceptor { event ->
                interceptedContents += event.message.contentRaw
                Interception.Pass
            }
        ),
        bEventConsumer = FakeBEventConsumer { _ -> }
    )

    private fun TestScope.eventsScope(): CoroutineScope = backgroundScope.coroutineContext.job
        .let(::SupervisorJob)
        .let(backgroundScope.coroutineContext::plus)
        .let(::CoroutineScope)

    private fun TestScope.startEvents(ioScope: CoroutineScope) {
        DiscordEvents(
            eventManager = eventManager,
            messageEventListener = messageEventListener,
            memberLeaves = memberLeaves,
            ioScope = ioScope
        )
        runCurrent()
    }

    private fun sentLeaves(): List<DiscordMemberLeave> {
        return generateSequence { memberLeaves.tryReceive().getOrNull() }.toList()
    }

    private fun memberLeft(jda: JDA, discordUserId: Long): GuildMemberRemoveEvent {
        return GuildMemberRemoveEvent(
            jda,
            0,
            jdaFake<Guild>(emptyMap()),
            jdaFake<User>(mapOf("getIdLong" to discordUserId)),
            null
        )
    }

    private fun directMessage(content: String): MessageReceivedEvent {
        val message: Message = jdaFake(
            mapOf(
                "getIdLong" to MESSAGE_ID,
                "getChannel" to jdaFake<MessageChannelUnion>(mapOf("getType" to ChannelType.PRIVATE)),
                "getAuthor" to jdaFake<User>(mapOf("isBot" to false)),
                "isWebhookMessage" to false,
                "getContentRaw" to content
            )
        )
        return MessageReceivedEvent(jdaFake(emptyMap()), 0, message)
    }

    @Test
    fun GIVEN_running_events_WHEN_jda_dispatches_a_direct_message_THEN_interceptors_get_it(): TestResult {
        return runTest {
            startEvents(eventsScope())

            eventManager.handle(directMessage("1234"))
            runCurrent()
            messageEventListener.coroutineContext.job.children.toList().joinAll()

            assertEquals(listOf("1234"), interceptedContents)
        }
    }

    @Test
    fun GIVEN_running_events_WHEN_jda_dispatches_a_member_leave_THEN_the_discord_id_is_sent(): TestResult {
        return runTest {
            startEvents(eventsScope())

            eventManager.handle(memberLeft(jdaFake(emptyMap()), STEVE_ID))
            runCurrent()

            assertEquals(listOf(DiscordMemberLeave(DiscordUserId(STEVE_ID))), sentLeaves())
        }
    }

    @Test
    fun GIVEN_replaced_session_WHEN_new_jda_dispatches_a_member_leave_THEN_both_leaves_are_sent(): TestResult {
        return runTest {
            startEvents(eventsScope())

            eventManager.handle(memberLeft(jdaFake(emptyMap()), STEVE_ID))
            eventManager.handle(memberLeft(jdaFake(emptyMap()), ALEX_ID))
            runCurrent()

            assertEquals(
                listOf(DiscordMemberLeave(DiscordUserId(STEVE_ID)), DiscordMemberLeave(DiscordUserId(ALEX_ID))),
                sentLeaves()
            )
        }
    }

    @Test
    fun GIVEN_events_scope_cancelled_WHEN_a_member_leaves_THEN_nothing_is_sent(): TestResult {
        return runTest {
            val ioScope = eventsScope()
            startEvents(ioScope)

            ioScope.cancel()
            runCurrent()
            eventManager.handle(memberLeft(jdaFake(emptyMap()), STEVE_ID))
            runCurrent()

            assertTrue(eventManager.registeredListeners.isEmpty())
            assertTrue(sentLeaves().isEmpty())
        }
    }

    private companion object {
        const val STEVE_ID = 4242L
        const val ALEX_ID = 4343L
        const val MESSAGE_ID = 900L
    }
}
