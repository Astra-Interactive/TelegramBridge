@file:Suppress("FunctionNaming")

package ru.astrainteractive.messagebridge.link.discord.command

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Role
import net.dv8tion.jda.api.entities.channel.ChannelType
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import net.dv8tion.jda.api.requests.restaction.AuditableRestAction
import net.dv8tion.jda.api.requests.restaction.CacheRestAction
import ru.astrainteractive.messagebridge.core.api.config.PluginConfiguration
import ru.astrainteractive.messagebridge.core.api.fake.FakeTranslationKrate
import ru.astrainteractive.messagebridge.core.api.util.toMessengerText
import ru.astrainteractive.messagebridge.link.api.config.LinkTranslation
import ru.astrainteractive.messagebridge.link.api.fake.FakeLinkApi
import ru.astrainteractive.messagebridge.link.api.mapping.asMessage
import ru.astrainteractive.messagebridge.link.api.model.LinkResponse
import ru.astrainteractive.messagebridge.link.api.player.model.LinkedPlayerModel
import ru.astrainteractive.messagebridge.link.discord.fake.BRIDGE_CHANNEL_ID
import ru.astrainteractive.messagebridge.link.discord.fake.MEMBER_ID
import ru.astrainteractive.messagebridge.link.discord.fake.MEMBER_NAME
import ru.astrainteractive.messagebridge.link.discord.fake.memberOf
import ru.astrainteractive.messagebridge.link.discord.fake.messageEventOf
import ru.astrainteractive.messagebridge.link.discord.internal.DiscordLinkRole
import ru.astrainteractive.messagebridge.messenger.discord.api.fake.RecordingDiscordMessageSender
import ru.astrainteractive.messagebridge.messenger.discord.api.fake.jdaFake
import ru.astrainteractive.messagebridge.messenger.discord.api.fake.jdaRequest
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DiscordLinkHandlerTest {
    private val translation = LinkTranslation()
    private val messageSender = RecordingDiscordMessageSender()
    private val givenRoles: MutableList<Role> = CopyOnWriteArrayList()
    private val role = jdaFake<Role> { method, _ -> if (method.name == "getName") "Linked" else null }
    private val linkedSteve = LinkedPlayerModel(
        uuid = UUID.fromString("069a79f4-44e9-4726-a5be-fca90e38aaf5"),
        lastMinecraftName = "Steve",
        discordLink = null,
        telegramLink = null
    )

    private fun guildOf(member: Member? = null): Guild = jdaFake { method, args ->
        when (method.name) {
            "getName" -> "Bridge"
            "getRoleById" -> if (args.first() == ROLE_ID) role else null
            "addRoleToMember" -> {
                givenRoles += args[1] as Role
                jdaRequest<AuditableRestAction<Void>>()
            }
            "retrieveMember" -> if (member == null) {
                jdaRequest<CacheRestAction<Member>>(failure = IllegalStateException("Unknown Member"))
            } else {
                jdaRequest<CacheRestAction<Member>>(value = member)
            }
            else -> null
        }
    }

    private fun jdaWithBridgeChannel(guild: Guild): JDA {
        val channel = jdaFake<TextChannel> { method, _ -> if (method.name == "getGuild") guild else null }
        return jdaFake { method, args ->
            if (method.name == "getTextChannelById" && args.first() == BRIDGE_CHANNEL_ID) channel else null
        }
    }

    private fun handlerOf(
        linkApi: FakeLinkApi,
        channelId: String = "$BRIDGE_CHANNEL_ID",
        link: PluginConfiguration.Link? = PluginConfiguration.Link(linkDiscordRole = "$ROLE_ID", linkLuckPermsRole = "")
    ): DiscordLinkHandler {
        val config = PluginConfiguration(jdaConfig = PluginConfiguration.JdaConfig(channelId = channelId), link = link)
        return DiscordLinkHandler(
            linkApi = linkApi,
            linkRole = DiscordLinkRole(),
            messageSender = messageSender,
            configFlow = MutableStateFlow(config),
            translationKrate = FakeTranslationKrate(translation)
        )
    }

    private fun answerOf(response: LinkResponse): String = response.asMessage(translation.link).toMessengerText()

    @Test
    fun GIVEN_member_in_the_bridge_channel_WHEN_linked_THEN_account_is_linked_and_told_the_result() = runTest {
        val linkApi = FakeLinkApi(response = LinkResponse.NoCode)

        handlerOf(linkApi).linkInChannel(1234, messageEventOf(text = "/link 1234"))

        assertEquals(listOf(1234), linkApi.discordCodes)
        val discordLink = LinkedPlayerModel.DiscordLink(lastDiscordName = MEMBER_NAME, discordId = MEMBER_ID)
        assertEquals(listOf(discordLink), linkApi.discordLinks)
        assertEquals(listOf(answerOf(LinkResponse.NoCode)), messageSender.replies)
    }

    @Test
    fun GIVEN_linking_gives_a_role_WHEN_member_is_linked_THEN_member_gets_the_role() = runTest {
        val linkApi = FakeLinkApi(response = LinkResponse.Linked(linkedSteve))
        val member = memberOf(guild = guildOf())

        handlerOf(linkApi).linkInChannel(1234, messageEventOf(text = "/link 1234", member = member))

        assertEquals(listOf(role), givenRoles)
        assertEquals(listOf(answerOf(LinkResponse.Linked(linkedSteve))), messageSender.replies)
    }

    @Test
    fun GIVEN_linking_gives_no_roles_WHEN_member_is_linked_THEN_no_role_is_given() = runTest {
        val linkApi = FakeLinkApi(response = LinkResponse.Linked(linkedSteve))
        val member = memberOf(guild = guildOf())

        handlerOf(linkApi, link = null).linkInChannel(1234, messageEventOf(text = "/link 1234", member = member))

        assertTrue(givenRoles.isEmpty())
        assertEquals(1, messageSender.replies.size)
    }

    @Test
    fun GIVEN_code_is_not_accepted_WHEN_member_is_linked_THEN_no_role_is_given() = runTest {
        val linkApi = FakeLinkApi(response = LinkResponse.AlreadyLinked)
        val member = memberOf(guild = guildOf())

        handlerOf(linkApi).linkInChannel(1234, messageEventOf(text = "/link 1234", member = member))

        assertTrue(givenRoles.isEmpty())
        assertEquals(listOf(answerOf(LinkResponse.AlreadyLinked)), messageSender.replies)
    }

    @Test
    fun GIVEN_message_in_the_channel_without_member_WHEN_linked_THEN_nothing_happens() = runTest {
        val linkApi = FakeLinkApi(response = LinkResponse.NoCode)

        handlerOf(linkApi).linkInChannel(1234, messageEventOf(text = "/link 1234", member = null))

        assertTrue(linkApi.discordCodes.isEmpty())
        assertTrue(messageSender.replies.isEmpty())
    }

    @Test
    fun GIVEN_direct_message_from_a_member_of_the_bridge_server_WHEN_linked_THEN_account_is_linked() = runTest {
        val linkApi = FakeLinkApi(response = LinkResponse.NoCode)
        val event = messageEventOf(
            text = "1234",
            channelType = ChannelType.PRIVATE,
            member = null,
            jda = jdaWithBridgeChannel(guildOf(member = memberOf()))
        )

        handlerOf(linkApi).linkFromPrivate(1234, event)

        assertEquals(listOf(1234), linkApi.discordCodes)
        assertEquals(listOf(answerOf(LinkResponse.NoCode)), messageSender.replies)
    }

    @Test
    fun GIVEN_direct_message_from_a_stranger_WHEN_linked_THEN_nothing_is_linked() = runTest {
        val linkApi = FakeLinkApi(response = LinkResponse.NoCode)
        val event = messageEventOf(
            text = "1234",
            channelType = ChannelType.PRIVATE,
            member = null,
            jda = jdaWithBridgeChannel(guildOf(member = null))
        )

        handlerOf(linkApi).linkFromPrivate(1234, event)

        assertTrue(linkApi.discordCodes.isEmpty())
        assertTrue(messageSender.replies.isEmpty())
    }

    @Test
    fun GIVEN_direct_message_and_no_bridge_channel_WHEN_linked_THEN_nothing_is_linked() = runTest {
        val linkApi = FakeLinkApi(response = LinkResponse.NoCode)
        val event = messageEventOf(
            text = "1234",
            channelType = ChannelType.PRIVATE,
            member = null,
            jda = jdaWithBridgeChannel(guildOf(member = memberOf()))
        )

        handlerOf(linkApi, channelId = "").linkFromPrivate(1234, event)

        assertTrue(linkApi.discordCodes.isEmpty())
        assertTrue(messageSender.replies.isEmpty())
    }

    private companion object {
        const val ROLE_ID = 77L
    }
}
