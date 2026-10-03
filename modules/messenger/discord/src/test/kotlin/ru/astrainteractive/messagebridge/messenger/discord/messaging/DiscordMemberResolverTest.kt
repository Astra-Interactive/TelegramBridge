@file:Suppress("FunctionNaming")

package ru.astrainteractive.messagebridge.messenger.discord.messaging

import kotlinx.coroutines.test.runTest
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import net.dv8tion.jda.api.requests.restaction.CacheRestAction
import ru.astrainteractive.klibs.mikro.core.util.tryCast
import ru.astrainteractive.messagebridge.messaging.model.Text
import ru.astrainteractive.messagebridge.messenger.discord.api.DiscordAuthorResolver
import ru.astrainteractive.messagebridge.messenger.discord.fake.JdaAnswer
import ru.astrainteractive.messagebridge.messenger.discord.fake.jdaFake
import java.util.function.Consumer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

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

    private fun channel(cached: Member?): TextChannel {
        val guild: Guild = jdaFake(
            mapOf(
                "getMemberById" to JdaAnswer { args ->
                    guildCalls += "getMemberById:${args.first()}"
                    cached
                },
                "retrieveMemberById" to JdaAnswer { args ->
                    guildCalls += "retrieveMemberById:${args.first()}"
                    memberRetrieval
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

    private companion object {
        const val DISCORD_ID = 4242L
    }
}
