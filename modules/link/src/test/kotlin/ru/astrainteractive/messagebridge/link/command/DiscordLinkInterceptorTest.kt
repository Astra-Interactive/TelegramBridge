@file:Suppress("FunctionNaming")

package ru.astrainteractive.messagebridge.link.command

import kotlinx.coroutines.test.runTest
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.channel.ChannelType
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import ru.astrainteractive.klibs.kstorage.api.asCachedKrate
import ru.astrainteractive.klibs.kstorage.api.impl.DefaultMutableKrate
import ru.astrainteractive.messagebridge.core.PluginConfiguration
import ru.astrainteractive.messagebridge.core.PluginTranslation
import ru.astrainteractive.messagebridge.core.mapping.toMessengerText
import ru.astrainteractive.messagebridge.link.api.internal.CodeApiImpl
import ru.astrainteractive.messagebridge.link.api.internal.LinkApiImpl
import ru.astrainteractive.messagebridge.link.api.model.CodeUser
import ru.astrainteractive.messagebridge.link.controller.DiscordRoleController
import ru.astrainteractive.messagebridge.link.controller.LuckPermsRoleController
import ru.astrainteractive.messagebridge.link.database.model.LinkedPlayerModel
import ru.astrainteractive.messagebridge.link.fake.FakeLinkingDao
import ru.astrainteractive.messagebridge.link.fake.FakeLuckPermsProvider
import ru.astrainteractive.messagebridge.messaging.model.Interception
import java.lang.reflect.Proxy
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class DiscordLinkInterceptorTest {
    private val translation = PluginTranslation()
    private val configKrate = DefaultMutableKrate(factory = { PluginConfiguration() }, loader = { null })
        .asCachedKrate()
    private val codeApi = CodeApiImpl()
    private val linkingDao = FakeLinkingDao()
    private val interceptor = DiscordLinkInterceptor(
        linkApi = LinkApiImpl(
            linkingDao = linkingDao,
            codeApi = codeApi,
            discordRoleController = DiscordRoleController(configKrate),
            luckPermsRoleController = LuckPermsRoleController(
                configKrate = configKrate,
                luckPermsProvider = FakeLuckPermsProvider()
            )
        ),
        translationKrate = DefaultMutableKrate(factory = { translation }, loader = { null }).asCachedKrate()
    )
    private val steve = CodeUser(name = "Steve", uuid = UUID.fromString("5e4a7f7a-0000-4000-8000-000000000002"))
    private val stevie: Member = jdaFake(mapOf("getIdLong" to DISCORD_ID, "getEffectiveName" to "Stevie"))

    private inline fun <reified T : Any> jdaFake(answerByGetter: Map<String, Any?>): T {
        val fake = Proxy.newProxyInstance(T::class.java.classLoader, arrayOf(T::class.java)) { _, method, _ ->
            check(method.name in answerByGetter) { "${T::class.simpleName}.${method.name} is not faked" }
            answerByGetter[method.name]
        }
        return fake as T
    }

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

    private companion object {
        const val DISCORD_ID = 4242L
        const val MESSAGE_ID = 900L
    }
}
