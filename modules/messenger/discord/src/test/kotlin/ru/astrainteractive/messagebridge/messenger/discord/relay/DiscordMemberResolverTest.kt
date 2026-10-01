@file:Suppress("FunctionNaming")

package ru.astrainteractive.messagebridge.messenger.discord.relay

import kotlinx.coroutines.test.runTest
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import net.dv8tion.jda.api.exceptions.ErrorResponseException
import net.dv8tion.jda.api.requests.ErrorResponse
import net.dv8tion.jda.api.requests.Response
import net.dv8tion.jda.api.requests.restaction.CacheRestAction
import ru.astrainteractive.messagebridge.link.player.LinkedPlayerModel
import ru.astrainteractive.messagebridge.messaging.model.Text
import ru.astrainteractive.messagebridge.messenger.discord.jdaFake
import ru.astrainteractive.messagebridge.messenger.discord.jdaRequest
import java.io.IOException
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame

class DiscordMemberResolverTest {
    private val steve = LinkedPlayerModel(
        uuid = UUID.fromString("069a79f4-44e9-4726-a5be-fca90e38aaf5"),
        lastMinecraftName = "Steve",
        discordLink = LinkedPlayerModel.DiscordLink(lastDiscordName = "steve", discordId = DISCORD_ID),
        telegramLink = null
    )
    private val message = Text.Minecraft(author = "Steve", uuid = "${steve.uuid}", text = "hi")
    private val retrievedIds = mutableListOf<Long>()

    private fun channel(retrieved: CacheRestAction<Member>): TextChannel {
        val guild = jdaFake<Guild> { method, args ->
            when (method.name) {
                "retrieveMemberById" -> retrieved.also { _ -> retrievedIds += args.first() as Long }
                else -> null
            }
        }
        return jdaFake { method, _ -> if (method.name == "getGuild") guild else null }
    }

    private fun unknown(errorResponse: ErrorResponse): ErrorResponseException {
        return ErrorResponseException.create(errorResponse, Response(null, 404, "Not Found", -1, emptySet()))
    }

    @Test
    fun GIVEN_linked_member_on_the_server_WHEN_resolved_THEN_the_member_is_shown() = runTest {
        val member = jdaFake<Member> { _, _ -> null }
        val resolver = DiscordMemberResolver(FakeLinkingDao(steve))

        val result = resolver.resolve(channel(jdaRequest(value = member)), message)

        assertSame(member, result.getOrThrow())
        assertEquals(listOf(DISCORD_ID), retrievedIds)
    }

    @Test
    fun GIVEN_linked_member_left_the_server_WHEN_resolved_THEN_the_message_goes_without_the_member() = runTest {
        val resolver = DiscordMemberResolver(FakeLinkingDao(steve))

        val result = resolver.resolve(channel(jdaRequest(failure = unknown(ErrorResponse.UNKNOWN_MEMBER))), message)

        assertNull(result.getOrThrow())
    }

    @Test
    fun GIVEN_linked_account_was_deleted_WHEN_resolved_THEN_the_message_goes_without_the_member() = runTest {
        val resolver = DiscordMemberResolver(FakeLinkingDao(steve))

        val result = resolver.resolve(channel(jdaRequest(failure = unknown(ErrorResponse.UNKNOWN_USER))), message)

        assertNull(result.getOrThrow())
    }

    @Test
    fun GIVEN_network_error_WHEN_resolved_THEN_the_failure_is_kept_so_the_message_is_tried_again() = runTest {
        val resolver = DiscordMemberResolver(FakeLinkingDao(steve))

        val result = resolver.resolve(channel(jdaRequest(failure = IOException("timeout"))), message)

        assertIs<IOException>(result.exceptionOrNull())
    }

    @Test
    fun GIVEN_author_without_linked_discord_WHEN_resolved_THEN_discord_is_not_asked() = runTest {
        val resolver = DiscordMemberResolver(FakeLinkingDao(steve.copy(discordLink = null)))

        val result = resolver.resolve(channel(jdaRequest(failure = IOException("unexpected"))), message)

        assertNull(result.getOrThrow())
        assertEquals(emptyList<Long>(), retrievedIds)
    }

    private companion object {
        const val DISCORD_ID = 42L
    }
}
