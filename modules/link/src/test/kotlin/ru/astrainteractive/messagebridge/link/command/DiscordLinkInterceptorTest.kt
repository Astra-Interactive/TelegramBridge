@file:Suppress("FunctionNaming")

package ru.astrainteractive.messagebridge.link.command

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.test.runTest
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.entities.channel.ChannelType
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.requests.restaction.CacheRestAction
import ru.astrainteractive.klibs.kstorage.api.asCachedKrate
import ru.astrainteractive.klibs.kstorage.api.impl.DefaultMutableKrate
import ru.astrainteractive.klibs.mikro.core.util.tryCast
import ru.astrainteractive.messagebridge.core.PluginConfiguration
import ru.astrainteractive.messagebridge.core.PluginTranslation
import ru.astrainteractive.messagebridge.core.mapping.toMessengerText
import ru.astrainteractive.messagebridge.link.code.internal.CodeApiImpl
import ru.astrainteractive.messagebridge.link.code.model.CodeUser
import ru.astrainteractive.messagebridge.link.fake.FakeLuckPermsProvider
import ru.astrainteractive.messagebridge.link.internal.DiscordRoleController
import ru.astrainteractive.messagebridge.link.internal.LuckPermsRoleController
import ru.astrainteractive.messagebridge.link.player.fake.FakeLinkingDao
import ru.astrainteractive.messagebridge.link.player.model.MessengerAccount
import ru.astrainteractive.messagebridge.link.usecase.LinkAccountUseCase
import ru.astrainteractive.messagebridge.messaging.model.Interception
import ru.astrainteractive.messagebridge.messenger.discord.fake.JdaAnswer
import ru.astrainteractive.messagebridge.messenger.discord.fake.jdaFake
import ru.astrainteractive.messagebridge.messenger.discord.model.DiscordRoleChange
import java.util.UUID
import java.util.function.Consumer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class DiscordLinkInterceptorTest {
    private val translation = PluginTranslation()
    private val configKrate = DefaultMutableKrate(
        factory = {
            PluginConfiguration(
                jdaConfig = PluginConfiguration.JdaConfig(channelId = BRIDGE_CHANNEL_ID),
                link = PluginConfiguration.Link(linkDiscordRole = "$ROLE_ID", linkLuckPermsRole = "verified")
            )
        },
        loader = { null }
    ).asCachedKrate()
    private val roleChanges = Channel<DiscordRoleChange>(Channel.UNLIMITED)
    private var memberOnServer: Member? = null
    private var memberRequestFailure: Throwable = IllegalStateException("10007: Unknown Member")
    private val memberRequest: CacheRestAction<Member> = jdaFake(
        mapOf(
            "queue" to JdaAnswer { args ->
                val member = memberOnServer
                if (member == null) {
                    args[1]
                        ?.tryCast<Consumer<Throwable>>()
                        ?.accept(memberRequestFailure)
                } else {
                    args.first()
                        ?.tryCast<Consumer<Member>>()
                        ?.accept(member)
                }
            }
        )
    )
    private val guild: Guild = jdaFake(
        mapOf(
            "getName" to "Bridge",
            "retrieveMemberById" to memberRequest
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
        discordRoleController = DiscordRoleController(configKrate = configKrate, roleChanges = roleChanges),
        configKrate = configKrate,
        translationKrate = DefaultMutableKrate(factory = { translation }, loader = { null }).asCachedKrate()
    )
    private val steve = CodeUser(name = "Steve", uuid = UUID.fromString("5e4a7f7a-0000-4000-8000-000000000002"))
    private val stevie: Member = jdaFake(
        mapOf("getIdLong" to DISCORD_ID, "getEffectiveName" to "Stevie", "getGuild" to guild)
    )
    private val stevieUser: User = jdaFake(mapOf("getIdLong" to DISCORD_ID, "getEffectiveName" to "stevie_global"))
    private val stevieGrant = DiscordRoleChange.Grant(discordUserId = DISCORD_ID, roleId = ROLE_ID)
    private val bridgeChannel: TextChannel = jdaFake(mapOf("getGuild" to guild))
    private var visibleBridgeChannel: TextChannel? = bridgeChannel
    private val jda: JDA = jdaFake(
        mapOf(
            "getTextChannelById" to JdaAnswer { args ->
                visibleBridgeChannel.takeIf { _ -> args.first() == BRIDGE_CHANNEL_ID }
            }
        )
    )

    private fun event(content: String, member: Member?, channelType: ChannelType): MessageReceivedEvent {
        val message: Message = jdaFake(
            mapOf(
                "getIdLong" to MESSAGE_ID,
                "getChannel" to jdaFake<MessageChannelUnion>(mapOf("getType" to channelType)),
                "getContentRaw" to content,
                "getMember" to member,
                "getAuthor" to stevieUser
            )
        )
        return MessageReceivedEvent(jda, 0, message)
    }

    @Test
    fun GIVEN_code_created_in_game_WHEN_member_sends_it_in_bridge_channel_THEN_discord_is_linked_and_reads_success() =
        runTest {
            val code = codeApi.generateCodeForPlayer(steve)

            val interception = interceptor.intercept(event("/link $code", stevie, ChannelType.TEXT))

            assertEquals(Interception.Reply(translation.link.success.toMessengerText()), interception)
            assertEquals(
                MessengerAccount.Discord(id = DISCORD_ID, name = "Stevie"),
                linkingDao.linkedPlayers[steve.uuid]?.discord
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
    fun GIVEN_code_created_in_game_WHEN_server_member_sends_it_in_a_direct_message_THEN_linked_with_the_role() =
        runTest {
            memberOnServer = stevie
            val code = codeApi.generateCodeForPlayer(steve)

            val interception = interceptor.intercept(event("$code", null, ChannelType.PRIVATE))

            assertEquals(Interception.Reply(translation.link.success.toMessengerText()), interception)
            assertEquals(
                MessengerAccount.Discord(id = DISCORD_ID, name = "Stevie"),
                linkingDao.linkedPlayers[steve.uuid]?.discord
            )
            assertEquals(stevieGrant, roleChanges.tryReceive().getOrNull())
        }

    @Test
    fun GIVEN_user_who_is_not_on_the_server_WHEN_sends_code_in_a_direct_message_THEN_refused_and_code_is_kept() =
        runTest {
            val code = codeApi.generateCodeForPlayer(steve)

            val interception = interceptor.intercept(event("$code", null, ChannelType.PRIVATE))

            assertEquals(Interception.Reply(translation.link.notServerMember.toMessengerText()), interception)
            assertTrue(linkingDao.linkedPlayers.isEmpty())
            assertEquals(steve, codeApi.findUserByCode(code))
            assertTrue(roleChanges.tryReceive().isFailure)
        }

    @Test
    fun GIVEN_user_refused_as_not_on_the_server_WHEN_joins_and_sends_the_same_code_THEN_linked_with_the_role() =
        runTest {
            val code = codeApi.generateCodeForPlayer(steve)
            interceptor.intercept(event("$code", null, ChannelType.PRIVATE))
            memberOnServer = stevie

            val interception = interceptor.intercept(event("$code", null, ChannelType.PRIVATE))

            assertEquals(Interception.Reply(translation.link.success.toMessengerText()), interception)
            assertEquals(stevieGrant, roleChanges.tryReceive().getOrNull())
        }

    @Test
    fun GIVEN_bridge_channel_the_bot_cannot_see_WHEN_code_is_sent_in_a_direct_message_THEN_refused_and_no_link() =
        runTest {
            visibleBridgeChannel = null
            memberOnServer = stevie
            val code = codeApi.generateCodeForPlayer(steve)

            val interception = interceptor.intercept(event("$code", null, ChannelType.PRIVATE))

            assertEquals(Interception.Reply(translation.link.notServerMember.toMessengerText()), interception)
            assertTrue(linkingDao.linkedPlayers.isEmpty())
        }

    @Test
    fun GIVEN_member_request_that_jda_cancels_WHEN_code_is_sent_in_a_direct_message_THEN_reads_error_and_no_link() =
        runTest {
            memberRequestFailure = CancellationException("RestAction has been cancelled")
            val code = codeApi.generateCodeForPlayer(steve)

            val interception = interceptor.intercept(event("$code", null, ChannelType.PRIVATE))

            assertEquals(Interception.Reply(translation.link.unknownError.toMessengerText()), interception)
            assertTrue(linkingDao.linkedPlayers.isEmpty())
            assertEquals(steve, codeApi.findUserByCode(code))
            assertTrue(roleChanges.tryReceive().isFailure)
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

        assertEquals(stevieGrant, roleChanges.tryReceive().getOrNull())
    }

    @Test
    fun GIVEN_code_nobody_created_WHEN_member_sends_it_THEN_member_gets_no_role() = runTest {
        interceptor.intercept(event("/link $UNKNOWN_CODE", stevie, ChannelType.TEXT))

        assertTrue(roleChanges.tryReceive().isFailure)
    }

    private companion object {
        const val ROLE_ID = 123456789012345678L
        const val BRIDGE_CHANNEL_ID = "555"
        const val UNKNOWN_CODE = 1234
        const val DISCORD_ID = 4242L
        const val MESSAGE_ID = 900L
    }
}
