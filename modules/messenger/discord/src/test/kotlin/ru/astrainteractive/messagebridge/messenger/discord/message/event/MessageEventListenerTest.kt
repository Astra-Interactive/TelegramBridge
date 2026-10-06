@file:Suppress("FunctionNaming")

package ru.astrainteractive.messagebridge.messenger.discord.message.event

import kotlinx.coroutines.job
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.test.runTest
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.MessageType
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.entities.channel.ChannelType
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction
import ru.astrainteractive.astralibs.server.bridge.PlatformServer
import ru.astrainteractive.astralibs.server.player.KPlayer
import ru.astrainteractive.astralibs.server.player.OnlineKPlayer
import ru.astrainteractive.klibs.kstorage.api.asCachedKrate
import ru.astrainteractive.klibs.kstorage.api.impl.DefaultMutableKrate
import ru.astrainteractive.klibs.mikro.core.util.tryCast
import ru.astrainteractive.messagebridge.core.api.config.PluginConfiguration
import ru.astrainteractive.messagebridge.core.api.config.PluginTranslation
import ru.astrainteractive.messagebridge.messenger.api.api.MessageInterceptor
import ru.astrainteractive.messagebridge.messenger.api.fake.FakeBEventConsumer
import ru.astrainteractive.messagebridge.messenger.api.model.BEvent
import ru.astrainteractive.messagebridge.messenger.api.model.Interception
import ru.astrainteractive.messagebridge.messenger.api.model.MessageRef
import ru.astrainteractive.messagebridge.messenger.api.model.Text
import ru.astrainteractive.messagebridge.messenger.discord.fake.JdaAnswer
import ru.astrainteractive.messagebridge.messenger.discord.fake.jdaFake
import ru.astrainteractive.messagebridge.messenger.discord.message.command.DiscordCommandHandler
import ru.astrainteractive.messagebridge.messenger.discord.message.internal.DiscordMessageSender
import ru.astrainteractive.messagebridge.messenger.discord.message.mapping.DiscordCommandMapper
import ru.astrainteractive.messagebridge.messenger.discord.message.mapping.DiscordMessageRelevanceMapper
import ru.astrainteractive.messagebridge.messenger.discord.message.mapping.DiscordReplyMapper
import java.util.UUID
import java.util.function.Consumer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

private object EmptyPlatformServer : PlatformServer {
    override fun getOnlinePlayers(): List<OnlineKPlayer> = emptyList()

    override fun findOnlinePlayer(uuid: UUID): OnlineKPlayer? = null

    override fun findOfflinePlayer(uuid: UUID): KPlayer? = null

    override fun findOnlinePlayer(name: String): OnlineKPlayer? = null

    override fun findOfflinePlayer(name: String): KPlayer? = null
}

class MessageEventListenerTest {
    private val configKrate = DefaultMutableKrate(
        factory = { PluginConfiguration(jdaConfig = PluginConfiguration.JdaConfig(channelId = BRIDGE_CHANNEL_ID)) },
        loader = { null }
    ).asCachedKrate()
    private val translationKrate = DefaultMutableKrate(factory = { PluginTranslation() }, loader = { null })
        .asCachedKrate()
    private val published = FakeBEventConsumer { _ -> }
    private val replies = mutableListOf<String>()
    private val interceptedContents = mutableListOf<String>()
    private val jda: JDA = jdaFake(emptyMap())
    private val sentMessage: Message = jdaFake(emptyMap())
    private val replyAction: MessageCreateAction = jdaFake(
        mapOf(
            "queue" to JdaAnswer { args ->
                args.first()
                    ?.tryCast<Consumer<Message>>()
                    ?.accept(sentMessage)
            }
        )
    )
    private val steve: User = jdaFake(mapOf("getName" to "steve", "getIdLong" to STEVE_ID, "isBot" to false))
    private val bot: User = jdaFake(mapOf("getName" to "OtherBot", "getIdLong" to BOT_ID, "isBot" to true))
    private val stevie: Member = jdaFake(mapOf("getNickname" to "Stevie"))

    private fun message(
        content: String,
        channelType: ChannelType = ChannelType.TEXT,
        channelId: String = BRIDGE_CHANNEL_ID,
        author: User = steve,
        member: Member? = stevie,
        isWebhook: Boolean = false
    ): Message = jdaFake(
        mapOf(
            "getIdLong" to MESSAGE_ID,
            "getChannel" to jdaFake<MessageChannelUnion>(mapOf("getType" to channelType)),
            "getChannelId" to channelId,
            "getAuthor" to author,
            "getMember" to member,
            "isWebhookMessage" to isWebhook,
            "getContentRaw" to content,
            "getType" to MessageType.DEFAULT,
            "reply" to JdaAnswer { args ->
                replies += args.first().toString()
                replyAction
            }
        )
    )

    private fun recording(interception: Interception) = MessageInterceptor<MessageReceivedEvent> { event ->
        interceptedContents += event.message.contentRaw
        interception
    }

    private fun listener(vararg interceptors: MessageInterceptor<MessageReceivedEvent>): MessageEventListener {
        val messageSender = DiscordMessageSender()
        return MessageEventListener(
            relevanceMapper = DiscordMessageRelevanceMapper(configKrate = configKrate),
            commandMapper = DiscordCommandMapper(),
            commandHandler = DiscordCommandHandler(
                messageSender = messageSender,
                platformServer = EmptyPlatformServer,
                translationKrate = translationKrate
            ),
            replyMapper = DiscordReplyMapper(),
            messageSender = messageSender,
            messageInterceptors = interceptors.toList(),
            bEventConsumer = published
        )
    }

    private suspend fun receive(listener: MessageEventListener, message: Message) {
        listener.onMessageReceived(MessageReceivedEvent(jda, 0, message))
        listener.coroutineContext.job.children.toList().joinAll()
    }

    @Test
    fun GIVEN_interceptor_that_passes_WHEN_bridge_message_arrives_THEN_it_is_relayed() = runTest {
        receive(listener(recording(Interception.Pass)), message(content = "hello"))

        val relayed = Text.Discord(
            author = "Stevie",
            text = "hello",
            authorId = STEVE_ID,
            reply = null,
            ref = MessageRef.Discord(messageId = MESSAGE_ID)
        )
        assertEquals(listOf<BEvent>(relayed), published.sent)
        assertEquals(listOf("hello"), interceptedContents)
        assertEquals(emptyList(), replies)
    }

    @Test
    fun GIVEN_interceptor_that_consumes_WHEN_bridge_message_arrives_THEN_nothing_is_relayed_or_answered() = runTest {
        receive(listener(recording(Interception.Consumed)), message(content = "/link 1234"))

        assertEquals(emptyList(), published.sent)
        assertEquals(emptyList(), replies)
    }

    @Test
    fun GIVEN_interceptor_that_replies_WHEN_bridge_message_arrives_THEN_message_is_answered_and_not_relayed() =
        runTest {
            receive(listener(recording(Interception.Reply("Your account is linked"))), message(content = "/link 1234"))

            assertEquals(listOf("Your account is linked"), replies)
            assertEquals(emptyList(), published.sent)
        }

    @Test
    fun GIVEN_interceptor_that_passes_WHEN_direct_message_arrives_THEN_it_is_never_relayed() = runTest {
        receive(
            listener(recording(Interception.Pass)),
            message(content = "1234", channelType = ChannelType.PRIVATE, member = null)
        )

        assertEquals(listOf("1234"), interceptedContents)
        assertEquals(emptyList(), published.sent)
        assertEquals(emptyList(), replies)
    }

    @Test
    fun GIVEN_interceptor_that_replies_WHEN_direct_message_arrives_THEN_answer_goes_to_that_message() = runTest {
        receive(
            listener(recording(Interception.Reply("Code not found"))),
            message(content = "1234", channelType = ChannelType.PRIVATE, member = null)
        )

        assertEquals(listOf("Code not found"), replies)
        assertEquals(emptyList(), published.sent)
    }

    @Test
    fun GIVEN_webhook_message_WHEN_it_arrives_THEN_interceptors_never_see_it() = runTest {
        receive(listener(recording(Interception.Pass)), message(content = "/link 1234", isWebhook = true))

        assertEquals(emptyList(), interceptedContents)
        assertEquals(emptyList(), published.sent)
    }

    @Test
    fun GIVEN_message_of_another_bot_WHEN_it_arrives_THEN_interceptors_never_see_it() = runTest {
        receive(listener(recording(Interception.Pass)), message(content = "/link 1234", author = bot))

        assertEquals(emptyList(), interceptedContents)
        assertEquals(emptyList(), published.sent)
    }

    @Test
    fun GIVEN_message_in_another_channel_WHEN_it_arrives_THEN_interceptors_never_see_it() = runTest {
        receive(listener(recording(Interception.Pass)), message(content = "/link 1234", channelId = OTHER_CHANNEL_ID))

        assertEquals(emptyList(), interceptedContents)
        assertEquals(emptyList(), published.sent)
    }

    @Test
    fun GIVEN_vanilla_command_WHEN_it_arrives_THEN_interceptors_never_see_it() = runTest {
        receive(listener(recording(Interception.Pass)), message(content = "!vanilla"))

        assertEquals(emptyList(), interceptedContents)
        assertEquals(emptyList(), published.sent)
        assertEquals(1, replies.size)
    }

    @Test
    fun GIVEN_interceptor_that_throws_WHEN_bridge_message_arrives_THEN_nothing_is_relayed_and_failure_is_uncaught() {
        val t = assertFailsWith<IllegalStateException> {
            runTest {
                receive(listener(MessageInterceptor { _ -> error("Database is locked") }), message(content = "/link 1"))

                assertEquals(emptyList(), published.sent)
                assertEquals(emptyList(), replies)
            }
        }

        assertEquals("Database is locked", t.message)
    }

    private companion object {
        const val BRIDGE_CHANNEL_ID = "555"
        const val OTHER_CHANNEL_ID = "556"
        const val MESSAGE_ID = 900L
        const val STEVE_ID = 42L
        const val BOT_ID = 99L
    }
}
