@file:Suppress("FunctionNaming")
@file:OptIn(ExperimentalCoroutinesApi::class)

package ru.astrainteractive.messagebridge.messenger.discord.event

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.job
import kotlinx.coroutines.joinAll
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
import ru.astrainteractive.messagebridge.core.api.config.PluginConfiguration
import ru.astrainteractive.messagebridge.core.api.config.PluginTranslation
import ru.astrainteractive.messagebridge.messenger.api.api.MessageInterceptor
import ru.astrainteractive.messagebridge.messenger.api.fake.FakeBEventConsumer
import ru.astrainteractive.messagebridge.messenger.api.model.Interception
import ru.astrainteractive.messagebridge.messenger.discord.api.DiscordMemberLeaveListener
import ru.astrainteractive.messagebridge.messenger.discord.fake.jdaFake
import ru.astrainteractive.messagebridge.messenger.discord.internal.DiscordRelayedMessageCache
import ru.astrainteractive.messagebridge.messenger.discord.message.command.DiscordCommandHandler
import ru.astrainteractive.messagebridge.messenger.discord.message.event.MessageEventListener
import ru.astrainteractive.messagebridge.messenger.discord.message.internal.DiscordMessageSender
import ru.astrainteractive.messagebridge.messenger.discord.message.mapping.DiscordCommandMapper
import ru.astrainteractive.messagebridge.messenger.discord.message.mapping.DiscordMessageRelevanceMapper
import ru.astrainteractive.messagebridge.messenger.discord.message.mapping.DiscordReplyMapper
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DiscordEventsTest {
    private val eventManager = InterfacedEventManager()
    private val interceptedContents = mutableListOf<String>()
    private val heardLeaves = mutableListOf<String>()
    private val uncaught = mutableListOf<Throwable>()
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
        .plus(CoroutineExceptionHandler { _, t -> uncaught += t })
        .let(::CoroutineScope)

    private fun TestScope.startEvents(
        ioScope: CoroutineScope,
        vararg memberLeaveListeners: DiscordMemberLeaveListener
    ) {
        DiscordEvents(
            eventManager = eventManager,
            messageEventListener = messageEventListener,
            memberLeaveListeners = memberLeaveListeners.toList(),
            ioScope = ioScope
        )
        runCurrent()
    }

    private fun recordingLeaves(name: String) = DiscordMemberLeaveListener { discordUserId ->
        heardLeaves += "$name:$discordUserId"
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
    fun GIVEN_running_events_WHEN_jda_dispatches_a_direct_message_THEN_interceptors_get_it() = runTest {
        startEvents(eventsScope())

        eventManager.handle(directMessage("1234"))
        runCurrent()
        messageEventListener.coroutineContext.job.children.toList().joinAll()

        assertEquals(listOf("1234"), interceptedContents)
    }

    @Test
    fun GIVEN_two_leave_listeners_WHEN_jda_dispatches_a_member_leave_THEN_both_hear_the_discord_id() = runTest {
        startEvents(eventsScope(), recordingLeaves("first"), recordingLeaves("second"))

        eventManager.handle(memberLeft(jdaFake(emptyMap()), STEVE_ID))
        runCurrent()

        assertEquals(listOf("first:$STEVE_ID", "second:$STEVE_ID"), heardLeaves)
    }

    @Test
    fun GIVEN_leave_listener_that_throws_WHEN_two_members_leave_THEN_the_second_leave_is_still_heard() = runTest {
        val failingOnSteve = DiscordMemberLeaveListener { discordUserId ->
            check(discordUserId != STEVE_ID) { "Database is locked" }
            heardLeaves += "$discordUserId"
        }
        startEvents(eventsScope(), failingOnSteve)
        val jda: JDA = jdaFake(emptyMap())

        eventManager.handle(memberLeft(jda, STEVE_ID))
        eventManager.handle(memberLeft(jda, ALEX_ID))
        runCurrent()

        assertEquals(listOf("$ALEX_ID"), heardLeaves)
        assertEquals(listOf("Database is locked"), uncaught.map { t -> t.message })
    }

    @Test
    fun GIVEN_session_that_was_replaced_WHEN_the_new_jda_dispatches_a_member_leave_THEN_it_is_heard() = runTest {
        startEvents(eventsScope(), recordingLeaves("listener"))

        eventManager.handle(memberLeft(jdaFake(emptyMap()), STEVE_ID))
        eventManager.handle(memberLeft(jdaFake(emptyMap()), ALEX_ID))
        runCurrent()

        assertEquals(listOf("listener:$STEVE_ID", "listener:$ALEX_ID"), heardLeaves)
    }

    @Test
    fun GIVEN_events_scope_cancelled_WHEN_a_member_leaves_THEN_nobody_hears_it() = runTest {
        val ioScope = eventsScope()
        startEvents(ioScope, recordingLeaves("listener"))

        ioScope.cancel()
        runCurrent()
        eventManager.handle(memberLeft(jdaFake(emptyMap()), STEVE_ID))
        runCurrent()

        assertTrue(eventManager.registeredListeners.isEmpty())
        assertTrue(heardLeaves.isEmpty())
    }

    private companion object {
        const val STEVE_ID = 4242L
        const val ALEX_ID = 4343L
        const val MESSAGE_ID = 900L
    }
}
