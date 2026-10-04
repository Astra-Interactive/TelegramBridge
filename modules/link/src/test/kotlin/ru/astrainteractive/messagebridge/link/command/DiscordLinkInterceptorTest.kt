@file:Suppress("FunctionNaming")

package ru.astrainteractive.messagebridge.link.command

import kotlinx.coroutines.test.runTest
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.Role
import net.dv8tion.jda.api.entities.channel.ChannelType
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.requests.restaction.AuditableRestAction
import ru.astrainteractive.klibs.kstorage.api.asCachedKrate
import ru.astrainteractive.klibs.kstorage.api.impl.DefaultMutableKrate
import ru.astrainteractive.messagebridge.core.PluginConfiguration
import ru.astrainteractive.messagebridge.core.PluginTranslation
import ru.astrainteractive.messagebridge.core.mapping.toMessengerText
import ru.astrainteractive.messagebridge.link.code.internal.CodeApiImpl
import ru.astrainteractive.messagebridge.link.code.model.CodeUser
import ru.astrainteractive.messagebridge.link.fake.FakeLuckPermsProvider
import ru.astrainteractive.messagebridge.link.internal.DiscordRoleController
import ru.astrainteractive.messagebridge.link.internal.LuckPermsRoleController
import ru.astrainteractive.messagebridge.link.player.fake.FakeLinkingDao
import ru.astrainteractive.messagebridge.link.player.model.LinkedPlayerModel
import ru.astrainteractive.messagebridge.link.usecase.LinkAccountUseCase
import ru.astrainteractive.messagebridge.messaging.model.Interception
import ru.astrainteractive.messagebridge.messenger.discord.fake.JdaAnswer
import ru.astrainteractive.messagebridge.messenger.discord.fake.jdaFake
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

class DiscordLinkInterceptorTest {
    private val translation = PluginTranslation()
    private val configKrate = DefaultMutableKrate(
        factory = {
            PluginConfiguration(
                link = PluginConfiguration.Link(linkDiscordRole = ROLE_ID, linkLuckPermsRole = "verified")
            )
        },
        loader = { null }
    ).asCachedKrate()
    private val grantedRoles = mutableListOf<Any?>()
    private val linkedRole: Role = jdaFake(emptyMap())
    private val guild: Guild = jdaFake(
        mapOf(
            "getRoleById" to JdaAnswer { args -> linkedRole.takeIf { _ -> args.first() == ROLE_ID } },
            "addRoleToMember" to JdaAnswer { args ->
                grantedRoles += args[1]
                jdaFake<AuditableRestAction<Void>>(mapOf("queue" to null))
            }
        )
    )
    private val codeApi = CodeApiImpl()
    private val linkingDao = FakeLinkingDao()
    private val interceptor = DiscordLinkInterceptor(
        linkAccountUseCase = LinkAccountUseCase(
            codeApi = codeApi,
            linkingDao = linkingDao,
            luckPermsRoleController = LuckPermsRoleController(
                configKrate = configKrate,
                luckPermsProvider = FakeLuckPermsProvider()
            )
        ),
        discordRoleController = DiscordRoleController(configKrate),
        translationKrate = DefaultMutableKrate(factory = { translation }, loader = { null }).asCachedKrate()
    )
    private val steve = CodeUser(name = "Steve", uuid = UUID.fromString("5e4a7f7a-0000-4000-8000-000000000002"))
    private val stevie: Member = jdaFake(
        mapOf("getIdLong" to DISCORD_ID, "getEffectiveName" to "Stevie", "getGuild" to guild)
    )

    private fun event(content: String, member: Member?, channelType: ChannelType): MessageReceivedEvent {
        val message: Message = jdaFake(
            mapOf(
                "getIdLong" to MESSAGE_ID,
                "getChannel" to jdaFake<MessageChannelUnion>(mapOf("getType" to channelType)),
                "getContentRaw" to content,
                "getMember" to member
            )
        )
        return MessageReceivedEvent(jdaFake<JDA>(emptyMap()), 0, message)
    }

    @Test
    fun GIVEN_code_created_in_game_WHEN_member_sends_it_in_bridge_channel_THEN_discord_is_linked_and_reads_success() =
        runTest {
            val code = codeApi.generateCodeForPlayer(steve)

            val interception = interceptor.intercept(event("/link $code", stevie, ChannelType.TEXT))

            assertEquals(Interception.Reply(translation.link.success.toMessengerText()), interception)
            assertEquals(
                LinkedPlayerModel.DiscordLink(lastDiscordName = "Stevie", discordId = DISCORD_ID),
                linkingDao.linkedPlayers[steve.uuid]?.discordLink
            )
        }

    @Test
    fun GIVEN_ordinary_text_in_bridge_channel_WHEN_intercepted_THEN_it_passes_and_nothing_is_linked() = runTest {
        codeApi.generateCodeForPlayer(steve)

        val interception = interceptor.intercept(event("hello", stevie, ChannelType.TEXT))

        assertEquals(Interception.Pass, interception)
        assertTrue(linkingDao.linkedPlayers.isEmpty())
    }

    @Test
    fun GIVEN_link_without_a_member_WHEN_intercepted_THEN_it_is_consumed_and_code_stays_valid() = runTest {
        val code = codeApi.generateCodeForPlayer(steve)

        val interception = interceptor.intercept(event("/link $code", null, ChannelType.TEXT))

        assertEquals(Interception.Consumed, interception)
        assertTrue(linkingDao.linkedPlayers.isEmpty())
        assertEquals(steve, codeApi.findUserByCode(code))
    }

    @Test
    fun GIVEN_direct_message_WHEN_intercepted_THEN_it_never_passes() = runTest {
        val code = codeApi.generateCodeForPlayer(steve)

        val codeInterception = interceptor.intercept(event("$code", null, ChannelType.PRIVATE))
        val textInterception = interceptor.intercept(event("hello", null, ChannelType.PRIVATE))

        assertNotEquals(Interception.Pass, codeInterception)
        assertNotEquals(Interception.Pass, textInterception)
    }

    @Test
    fun GIVEN_code_created_in_game_WHEN_member_links_THEN_member_gets_the_linked_role() = runTest {
        val code = codeApi.generateCodeForPlayer(steve)

        interceptor.intercept(event("/link $code", stevie, ChannelType.TEXT))

        assertSame(linkedRole, grantedRoles.single())
    }

    @Test
    fun GIVEN_code_nobody_created_WHEN_member_sends_it_THEN_member_gets_no_role() = runTest {
        interceptor.intercept(event("/link $UNKNOWN_CODE", stevie, ChannelType.TEXT))

        assertTrue(grantedRoles.isEmpty())
    }

    private companion object {
        const val ROLE_ID = "123456789012345678"
        const val UNKNOWN_CODE = 1234
        const val DISCORD_ID = 4242L
        const val MESSAGE_ID = 900L
    }
}
