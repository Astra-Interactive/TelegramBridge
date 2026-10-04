@file:Suppress("FunctionNaming")

package ru.astrainteractive.messagebridge.link.internal

import kotlinx.coroutines.test.runTest
import net.dv8tion.jda.api.entities.Member
import ru.astrainteractive.klibs.kstorage.api.asCachedKrate
import ru.astrainteractive.klibs.kstorage.api.impl.DefaultMutableKrate
import ru.astrainteractive.messagebridge.core.PluginConfiguration
import ru.astrainteractive.messagebridge.link.code.internal.CodeApiImpl
import ru.astrainteractive.messagebridge.link.code.model.CodeUser
import ru.astrainteractive.messagebridge.link.fake.FakeLuckPermsProvider
import ru.astrainteractive.messagebridge.link.model.LinkResponse
import ru.astrainteractive.messagebridge.link.player.fake.FakeLinkingDao
import ru.astrainteractive.messagebridge.link.player.model.LinkedPlayerModel
import ru.astrainteractive.messagebridge.messenger.discord.fake.jdaFake
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LinkApiImplTest {
    private val configKrate = DefaultMutableKrate(factory = { PluginConfiguration() }, loader = { null })
        .asCachedKrate()
    private val codeApi = CodeApiImpl()
    private val linkingDao = FakeLinkingDao()
    private val linkApi = LinkApiImpl(
        linkingDao = linkingDao,
        codeApi = codeApi,
        discordRoleController = DiscordRoleController(configKrate),
        luckPermsRoleController = LuckPermsRoleController(
            configKrate = configKrate,
            luckPermsProvider = FakeLuckPermsProvider()
        )
    )
    private val steve = CodeUser(name = "Steve", uuid = UUID.fromString("5e4a7f7a-0000-4000-8000-000000000002"))
    private val stevie: Member = jdaFake(mapOf("getIdLong" to DISCORD_ID, "getEffectiveName" to "Stevie"))
    private val stevieDiscordLink = LinkedPlayerModel.DiscordLink(lastDiscordName = "Stevie", discordId = DISCORD_ID)

    @Test
    fun GIVEN_code_of_a_player_WHEN_discord_member_sends_it_THEN_member_is_linked_to_that_player() = runTest {
        val code = codeApi.generateCodeForPlayer(steve)

        val response = linkApi.linkDiscord(code, stevie)

        val linked = LinkedPlayerModel(uuid = steve.uuid, lastMinecraftName = "Steve", discordLink = stevieDiscordLink)
        assertEquals(LinkResponse.Linked(linked), response)
        assertEquals(linked, linkingDao.linkedPlayers[steve.uuid])
    }

    @Test
    fun GIVEN_code_nobody_created_WHEN_discord_member_sends_it_THEN_no_code_and_nothing_is_linked() = runTest {
        val response = linkApi.linkDiscord(UNKNOWN_CODE, stevie)

        assertEquals(LinkResponse.NoCode, response)
        assertTrue(linkingDao.linkedPlayers.isEmpty())
    }

    @Test
    fun GIVEN_player_already_linked_to_discord_WHEN_another_member_sends_code_THEN_already_linked_and_link_stays() =
        runTest {
            val existing = LinkedPlayerModel.DiscordLink(lastDiscordName = "Old", discordId = OTHER_DISCORD_ID)
            linkingDao.upsert(LinkedPlayerModel(uuid = steve.uuid, lastMinecraftName = "Steve", discordLink = existing))
            val code = codeApi.generateCodeForPlayer(steve)

            val response = linkApi.linkDiscord(code, stevie)

            assertEquals(LinkResponse.AlreadyLinked, response)
            assertEquals(existing, linkingDao.linkedPlayers[steve.uuid]?.discordLink)
        }

    @Test
    fun GIVEN_unreadable_database_WHEN_discord_member_sends_code_THEN_unknown_error_and_nothing_is_linked() = runTest {
        val code = codeApi.generateCodeForPlayer(steve)
        linkingDao.findFailure = IllegalStateException("Database is locked")

        val response = linkApi.linkDiscord(code, stevie)

        assertEquals(LinkResponse.UnknownError, response)
        assertTrue(linkingDao.linkedPlayers.isEmpty())
    }

    private companion object {
        const val DISCORD_ID = 4242L
        const val OTHER_DISCORD_ID = 4343L
        const val UNKNOWN_CODE = 1234
    }
}
