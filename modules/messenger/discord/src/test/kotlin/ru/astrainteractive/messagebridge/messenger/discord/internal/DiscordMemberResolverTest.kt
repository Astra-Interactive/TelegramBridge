@file:Suppress("FunctionNaming")

package ru.astrainteractive.messagebridge.messenger.discord.internal

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import net.dv8tion.jda.api.exceptions.ErrorResponseException
import net.dv8tion.jda.api.requests.ErrorResponse
import net.dv8tion.jda.api.requests.Response
import net.dv8tion.jda.api.requests.restaction.CacheRestAction
import ru.astrainteractive.klibs.mikro.core.util.tryCast
import ru.astrainteractive.messagebridge.messaging.model.Text
import ru.astrainteractive.messagebridge.messenger.discord.api.DiscordAuthorResolver
import ru.astrainteractive.messagebridge.messenger.discord.fake.JdaAnswer
import ru.astrainteractive.messagebridge.messenger.discord.fake.jdaFake
import java.net.SocketTimeoutException
import java.util.function.Consumer
import java.util.logging.Handler
import java.util.logging.Level
import java.util.logging.LogRecord
import java.util.logging.Logger
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class DiscordMemberResolverTest {
    private val steveText = Text.Minecraft(author = "Steve", uuid = "8667ba71-b85a-4004-af54-457a9734eed7", text = "hi")
    private val resolvedTexts = mutableListOf<Text>()
    private val guildCalls = mutableListOf<String>()
    private val cachedMember: Member = jdaFake(emptyMap())
    private val fetchedMember: Member = jdaFake(emptyMap())
    private val memberRetrieval: CacheRestAction<Member> = jdaFake(
        mapOf(
            "queue" to JdaAnswer { args ->
                args.first()
                    ?.tryCast<Consumer<Member>>()
                    ?.accept(fetchedMember)
            }
        )
    )

    private val failedRetrieval: CacheRestAction<Member> = failingRetrieval(
        ErrorResponseException.create(ErrorResponse.UNKNOWN_MEMBER, Response(null, 404, "Not Found", -1, emptySet()))
    )

    private val cancelledRetrieval: CacheRestAction<Member> = jdaFake(
        mapOf(
            "queue" to JdaAnswer { args ->
                args[1]
                    ?.tryCast<Consumer<Throwable>>()
                    ?.accept(CancellationException("RestAction has been cancelled"))
            }
        )
    )
    private val logRecords = mutableListOf<LogRecord>()
    private val recordingHandler = object : Handler() {
        override fun publish(record: LogRecord) {
            logRecords += record
        }

        override fun flush() = Unit

        override fun close() = Unit
    }
    private val resolverLogger: Logger = Logger.getLogger("MessageBridge-DiscordMemberResolver")

    @BeforeTest
    fun attachLogHandler() {
        resolverLogger.addHandler(recordingHandler)
    }

    @AfterTest
    fun detachLogHandler() {
        resolverLogger.removeHandler(recordingHandler)
    }

    private fun failingRetrieval(t: Throwable): CacheRestAction<Member> = jdaFake(
        mapOf(
            "queue" to JdaAnswer { args ->
                args[1]
                    ?.tryCast<Consumer<Throwable>>()
                    ?.accept(t)
            }
        )
    )

    private fun channel(cached: Member?, retrieval: CacheRestAction<Member> = memberRetrieval): TextChannel {
        val guild: Guild = jdaFake(
            mapOf(
                "getMemberById" to JdaAnswer { args ->
                    guildCalls += "getMemberById:${args.first()}"
                    cached
                },
                "retrieveMemberById" to JdaAnswer { args ->
                    guildCalls += "retrieveMemberById:${args.first()}"
                    retrieval
                }
            )
        )
        return jdaFake(mapOf("getGuild" to guild))
    }

    private fun resolver(discordId: Long?): DiscordMemberResolver {
        return DiscordMemberResolver(
            DiscordAuthorResolver { text ->
                resolvedTexts += text
                discordId
            }
        )
    }

    @Test
    fun GIVEN_author_without_discord_id_WHEN_resolved_THEN_no_member_and_guild_is_never_asked() = runTest {
        val member = resolver(discordId = null).resolve(jdaFake(emptyMap()), steveText)

        assertNull(member)
        assertEquals(listOf<Text>(steveText), resolvedTexts)
    }

    @Test
    fun GIVEN_author_whose_member_is_cached_WHEN_resolved_THEN_cached_member_is_used() = runTest {
        val member = resolver(discordId = DISCORD_ID).resolve(channel(cached = cachedMember), steveText)

        assertSame(cachedMember, member)
        assertEquals(listOf("getMemberById:$DISCORD_ID"), guildCalls)
    }

    @Test
    fun GIVEN_author_whose_member_is_not_cached_WHEN_resolved_THEN_member_is_fetched_from_discord() = runTest {
        val member = resolver(discordId = DISCORD_ID).resolve(channel(cached = null), steveText)

        assertSame(fetchedMember, member)
        assertEquals(listOf("getMemberById:$DISCORD_ID", "retrieveMemberById:$DISCORD_ID"), guildCalls)
    }

    @Test
    fun GIVEN_linked_author_who_left_the_server_WHEN_resolved_THEN_no_member_instead_of_a_failure() = runTest {
        val channel = channel(cached = null, retrieval = failedRetrieval)

        val member = resolver(discordId = DISCORD_ID).resolve(channel, steveText)

        assertNull(member)
    }

    @Test
    fun GIVEN_linked_author_who_left_the_server_WHEN_resolved_THEN_it_is_logged_as_not_on_the_server() = runTest {
        val channel = channel(cached = null, retrieval = failedRetrieval)

        resolver(discordId = DISCORD_ID).resolve(channel, steveText)

        assertEquals(listOf(Level.INFO), logRecords.map { record -> record.level })
        assertTrue("not on the server" in logRecords.single().message)
    }

    @Test
    fun GIVEN_member_request_that_jda_cancels_WHEN_resolved_THEN_no_member_and_a_warning_not_a_missing_member() =
        runTest {
            val channel = channel(cached = null, retrieval = cancelledRetrieval)

            val member = resolver(discordId = DISCORD_ID).resolve(channel, steveText)

            assertNull(member)
            assertEquals(listOf(Level.WARNING), logRecords.map { record -> record.level })
            assertFalse("not on the server" in logRecords.single().message)
        }

    @Test
    fun GIVEN_member_request_that_times_out_WHEN_resolved_THEN_no_member_and_a_warning_with_the_reason() = runTest {
        val channel = channel(cached = null, retrieval = failingRetrieval(SocketTimeoutException("Read timed out")))

        val member = resolver(discordId = DISCORD_ID).resolve(channel, steveText)

        assertNull(member)
        assertEquals(listOf(Level.WARNING), logRecords.map { record -> record.level })
        assertTrue("Read timed out" in logRecords.single().message)
    }

    @Test
    fun GIVEN_discord_server_error_WHEN_resolved_THEN_no_member_and_a_warning_not_a_missing_member() = runTest {
        val serverError = ErrorResponseException.create(
            ErrorResponse.SERVER_ERROR,
            Response(null, 500, "Internal Server Error", -1, emptySet())
        )
        val channel = channel(cached = null, retrieval = failingRetrieval(serverError))

        val member = resolver(discordId = DISCORD_ID).resolve(channel, steveText)

        assertNull(member)
        assertEquals(listOf(Level.WARNING), logRecords.map { record -> record.level })
        assertFalse("not on the server" in logRecords.single().message)
    }

    private companion object {
        const val DISCORD_ID = 4242L
    }
}
