@file:Suppress("FunctionNaming")

package ru.astrainteractive.messagebridge.messenger.discord.event

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent
import ru.astrainteractive.messagebridge.core.api.config.PluginConfiguration
import ru.astrainteractive.messagebridge.link.fake.RecordingDiscordMembership
import ru.astrainteractive.messagebridge.messenger.discord.channel.internal.DiscordChannelProvider
import ru.astrainteractive.messagebridge.messenger.discord.connection.model.DiscordConnection
import ru.astrainteractive.messagebridge.messenger.discord.fake.jdaFake
import kotlin.test.Test
import kotlin.test.assertEquals

class DiscordMemberLeaveListenerTest {
    private val membership = RecordingDiscordMembership()
    private val bridgeGuild = guild(BRIDGE_GUILD_ID)
    private val bridgeChannel = jdaFake<TextChannel> { method, _ ->
        if (method.name == "getGuild") bridgeGuild else null
    }
    private val jda = jdaFake<JDA> { method, args ->
        if (method.name == "getTextChannelById" && args.first() == BRIDGE_CHANNEL_ID) bridgeChannel else null
    }
    private val member = jdaFake<User> { method, _ ->
        when (method.name) {
            "getIdLong" -> MEMBER_ID
            "getId" -> "$MEMBER_ID"
            else -> null
        }
    }

    private fun guild(id: Long): Guild = jdaFake { method, _ ->
        when (method.name) {
            "getIdLong" -> id
            "getId" -> "$id"
            else -> null
        }
    }

    private fun listener(scope: CoroutineScope, channelId: String): DiscordMemberLeaveListener {
        val config = PluginConfiguration(jdaConfig = PluginConfiguration.JdaConfig(channelId = channelId))
        return DiscordMemberLeaveListener(
            channelProvider = DiscordChannelProvider(
                connection = MutableStateFlow(DiscordConnection.Connecting),
                webhookClients = emptyFlow(),
                configFlow = MutableStateFlow(config)
            ),
            discordMembership = membership,
            scope = scope
        )
    }

    private fun leave(guild: Guild): GuildMemberRemoveEvent {
        return GuildMemberRemoveEvent(jda, 0, guild, member, null)
    }

    @Test
    fun GIVEN_member_of_the_bridge_server_WHEN_left_THEN_the_link_is_revoked() = runTest {
        listener(this, "$BRIDGE_CHANNEL_ID").onGuildMemberRemove(leave(bridgeGuild))
        advanceUntilIdle()

        assertEquals(listOf(MEMBER_ID), membership.leftDiscordIds)
    }

    @Test
    fun GIVEN_member_of_another_server_of_the_bot_WHEN_left_THEN_the_link_is_kept() = runTest {
        listener(this, "$BRIDGE_CHANNEL_ID").onGuildMemberRemove(leave(guild(OTHER_GUILD_ID)))
        advanceUntilIdle()

        assertEquals(emptyList<Long>(), membership.leftDiscordIds)
    }

    @Test
    fun GIVEN_bridge_channel_is_not_set_WHEN_member_left_THEN_the_link_is_kept() = runTest {
        listener(this, "").onGuildMemberRemove(leave(bridgeGuild))
        advanceUntilIdle()

        assertEquals(emptyList<Long>(), membership.leftDiscordIds)
    }

    private companion object {
        const val BRIDGE_GUILD_ID = 1L
        const val OTHER_GUILD_ID = 2L
        const val BRIDGE_CHANNEL_ID = 10L
        const val MEMBER_ID = 42L
    }
}
