@file:Suppress("FunctionNaming")

package ru.astrainteractive.messagebridge.messenger.discord.impl.internal

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import net.dv8tion.jda.api.requests.GatewayIntent
import net.dv8tion.jda.api.utils.concurrent.Task
import ru.astrainteractive.messagebridge.core.api.config.PluginConfiguration
import ru.astrainteractive.messagebridge.link.fake.RecordingDiscordMembership
import ru.astrainteractive.messagebridge.messenger.discord.api.fake.jdaFake
import ru.astrainteractive.messagebridge.messenger.discord.api.model.DiscordConnection
import ru.astrainteractive.messagebridge.messenger.discord.impl.channel.internal.DiscordChannelProvider
import java.util.EnumSet
import java.util.function.Consumer
import kotlin.test.Test
import kotlin.test.assertEquals

class DiscordMemberSweepTest {
    private val membership = RecordingDiscordMembership()
    private var membersLoaded = 0

    private fun member(id: Long): Member = jdaFake { method, _ -> if (method.name == "getIdLong") id else null }

    @Suppress("UNCHECKED_CAST")
    private fun loading(members: List<Member>?, failure: Throwable?): Task<List<Member>> = jdaFake { method, args ->
        when {
            method.name == "onSuccess" && members != null -> (args.first() as Consumer<List<Member>>).accept(members)
            method.name == "onError" && failure != null -> (args.first() as Consumer<Throwable>).accept(failure)
        }
        null
    }

    private fun jda(intents: EnumSet<GatewayIntent>, task: Task<List<Member>>): JDA {
        val guild = jdaFake<Guild> { method, _ ->
            when (method.name) {
                "loadMembers" -> task.also { _ -> membersLoaded++ }
                "getName" -> "Bridge"
                else -> null
            }
        }
        val channel = jdaFake<TextChannel> { method, _ -> if (method.name == "getGuild") guild else null }
        return jdaFake { method, args ->
            when (method.name) {
                "getGatewayIntents" -> intents
                "getTextChannelById" -> channel.takeIf { _ -> args.first() == BRIDGE_CHANNEL_ID }
                else -> null
            }
        }
    }

    private fun sweep(channelId: String = "$BRIDGE_CHANNEL_ID"): DiscordMemberSweep {
        val config = PluginConfiguration(jdaConfig = PluginConfiguration.JdaConfig(channelId = channelId))
        return DiscordMemberSweep(
            channelProvider = DiscordChannelProvider(
                connection = MutableStateFlow(DiscordConnection.Connecting),
                webhookClients = emptyFlow(),
                configFlow = MutableStateFlow(config)
            ),
            discordMembership = membership
        )
    }

    @Test
    fun GIVEN_bot_sees_the_members_WHEN_swept_THEN_membership_compares_everyone_on_the_bridge_server() = runTest {
        val task = loading(members = listOf(member(MEMBER_ID), member(BOT_ID)), failure = null)

        sweep().sweep(jda(EnumSet.of(GatewayIntent.GUILD_MEMBERS), task))

        assertEquals(listOf(setOf(MEMBER_ID, BOT_ID)), membership.comparedMemberIds)
    }

    @Test
    fun GIVEN_bot_did_not_ask_for_members_WHEN_swept_THEN_nobody_is_compared() = runTest {
        val task = loading(members = listOf(member(BOT_ID)), failure = null)

        sweep().sweep(jda(EnumSet.of(GatewayIntent.MESSAGE_CONTENT), task))

        assertEquals(emptyList<Set<Long>>(), membership.comparedMemberIds)
        assertEquals(0, membersLoaded)
    }

    @Test
    fun GIVEN_bridge_channel_is_not_set_WHEN_swept_THEN_nobody_is_compared() = runTest {
        val task = loading(members = listOf(member(BOT_ID)), failure = null)

        sweep(channelId = "").sweep(jda(EnumSet.of(GatewayIntent.GUILD_MEMBERS), task))

        assertEquals(emptyList<Set<Long>>(), membership.comparedMemberIds)
    }

    @Test
    fun GIVEN_members_cannot_be_loaded_WHEN_swept_THEN_nobody_is_compared() = runTest {
        val task = loading(members = null, failure = IllegalStateException("timed out"))

        sweep().sweep(jda(EnumSet.of(GatewayIntent.GUILD_MEMBERS), task))

        assertEquals(emptyList<Set<Long>>(), membership.comparedMemberIds)
    }

    private companion object {
        const val BRIDGE_CHANNEL_ID = 10L
        const val MEMBER_ID = 42L
        const val BOT_ID = 100L
    }
}
