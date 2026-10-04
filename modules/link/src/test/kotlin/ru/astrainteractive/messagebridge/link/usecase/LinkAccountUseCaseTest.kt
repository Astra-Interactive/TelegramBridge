@file:Suppress("FunctionNaming")

package ru.astrainteractive.messagebridge.link.usecase

import kotlinx.coroutines.test.runTest
import ru.astrainteractive.klibs.kstorage.api.asCachedKrate
import ru.astrainteractive.klibs.kstorage.api.impl.DefaultMutableKrate
import ru.astrainteractive.messagebridge.core.PluginConfiguration
import ru.astrainteractive.messagebridge.link.code.internal.CodeApiImpl
import ru.astrainteractive.messagebridge.link.code.model.CodeUser
import ru.astrainteractive.messagebridge.link.fake.FakeLuckPermsProvider
import ru.astrainteractive.messagebridge.link.internal.LuckPermsRoleController
import ru.astrainteractive.messagebridge.link.model.LinkResponse
import ru.astrainteractive.messagebridge.link.player.fake.FakeLinkingDao
import ru.astrainteractive.messagebridge.link.player.model.LinkedPlayer
import ru.astrainteractive.messagebridge.link.player.model.MessengerAccount
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LinkAccountUseCaseTest {
    private val configKrate = DefaultMutableKrate(
        factory = {
            PluginConfiguration(
                link = PluginConfiguration.Link(linkDiscordRole = "123456789012345678", linkLuckPermsRole = "verified")
            )
        },
        loader = { null }
    ).asCachedKrate()
    private val codeApi = CodeApiImpl()
    private val linkingDao = FakeLinkingDao()
    private val luckPermsProvider = FakeLuckPermsProvider()
    private val useCase = LinkAccountUseCase(
        codeApi = codeApi,
        linkingDao = linkingDao,
        luckPermsRoleController = LuckPermsRoleController(
            configKrate = configKrate,
            luckPermsProvider = luckPermsProvider
        )
    )
    private val steve = CodeUser(name = "Steve", uuid = UUID.fromString("5e4a7f7a-0000-4000-8000-000000000002"))
    private val stevie = MessengerAccount.Discord(id = DISCORD_ID, name = "Stevie")
    private val steveTelegram = MessengerAccount.Telegram(id = TELEGRAM_ID, username = "steve_tg")
    private val alexUuid = UUID.fromString("5e4a7f7a-0000-4000-8000-000000000003")

    @Test
    fun GIVEN_code_of_a_player_WHEN_discord_account_sends_it_THEN_account_is_linked_to_that_player() = runTest {
        val code = codeApi.generateCodeForPlayer(steve)

        val response = useCase.link(code, stevie)

        assertEquals(LinkResponse.Linked, response)
        assertEquals(
            LinkedPlayer(uuid = steve.uuid, minecraftName = "Steve", discord = stevie, telegram = null),
            linkingDao.linkedPlayers[steve.uuid]
        )
    }

    @Test
    fun GIVEN_player_linked_to_discord_WHEN_telegram_account_sends_a_new_code_THEN_both_accounts_stay_linked() =
        runTest {
            useCase.link(codeApi.generateCodeForPlayer(steve), stevie)

            val response = useCase.link(codeApi.generateCodeForPlayer(steve), steveTelegram)

            assertEquals(LinkResponse.Linked, response)
            assertEquals(
                LinkedPlayer(uuid = steve.uuid, minecraftName = "Steve", discord = stevie, telegram = steveTelegram),
                linkingDao.linkedPlayers[steve.uuid]
            )
        }

    @Test
    fun GIVEN_account_linked_WHEN_link_succeeds_THEN_luckperms_group_is_requested() = runTest {
        useCase.link(codeApi.generateCodeForPlayer(steve), stevie)

        assertEquals(1, luckPermsProvider.provideCallCount)
    }

    @Test
    fun GIVEN_code_nobody_created_WHEN_account_sends_it_THEN_no_code_and_nothing_is_linked() = runTest {
        val response = useCase.link(UNKNOWN_CODE, stevie)

        assertEquals(LinkResponse.NoCode, response)
        assertTrue(linkingDao.linkedPlayers.isEmpty())
        assertEquals(0, luckPermsProvider.provideCallCount)
    }

    @Test
    fun GIVEN_player_already_linked_to_discord_WHEN_another_discord_account_sends_code_THEN_already_linked() =
        runTest {
            val existing = MessengerAccount.Discord(id = OTHER_DISCORD_ID, name = "Old")
            linkingDao.link(uuid = steve.uuid, minecraftName = "Steve", account = existing)
            val code = codeApi.generateCodeForPlayer(steve)

            val response = useCase.link(code, stevie)

            assertEquals(LinkResponse.AlreadyLinked, response)
            assertEquals(existing, linkingDao.linkedPlayers[steve.uuid]?.discord)
        }

    @Test
    fun GIVEN_unreadable_database_WHEN_account_sends_code_THEN_unknown_error_and_nothing_is_linked() = runTest {
        val code = codeApi.generateCodeForPlayer(steve)
        linkingDao.findFailure = IllegalStateException("Database is locked")

        val response = useCase.link(code, stevie)

        assertEquals(LinkResponse.UnknownError, response)
        assertTrue(linkingDao.linkedPlayers.isEmpty())
    }

    @Test
    fun GIVEN_database_that_can_not_write_WHEN_account_sends_code_THEN_unknown_error_and_no_group_is_requested() =
        runTest {
            val code = codeApi.generateCodeForPlayer(steve)
            linkingDao.linkFailure = IllegalStateException("Database is read-only")

            val response = useCase.link(code, stevie)

            assertEquals(LinkResponse.UnknownError, response)
            assertEquals(0, luckPermsProvider.provideCallCount)
        }

    @Test
    fun GIVEN_discord_account_linked_to_another_player_WHEN_it_sends_a_code_THEN_account_taken_and_links_stay() =
        runTest {
            linkingDao.link(uuid = alexUuid, minecraftName = "Alex", account = stevie)
            val code = codeApi.generateCodeForPlayer(steve)

            val response = useCase.link(code, stevie)

            assertEquals(LinkResponse.AccountTaken, response)
            assertEquals(null, linkingDao.linkedPlayers[steve.uuid])
            assertEquals(stevie, linkingDao.linkedPlayers[alexUuid]?.discord)
        }

    @Test
    fun GIVEN_telegram_account_linked_to_another_player_WHEN_it_sends_a_code_THEN_account_taken() = runTest {
        linkingDao.link(uuid = alexUuid, minecraftName = "Alex", account = steveTelegram)
        val code = codeApi.generateCodeForPlayer(steve)

        val response = useCase.link(code, steveTelegram)

        assertEquals(LinkResponse.AccountTaken, response)
        assertEquals(0, luckPermsProvider.provideCallCount)
    }

    private companion object {
        const val DISCORD_ID = 4242L
        const val OTHER_DISCORD_ID = 4343L
        const val TELEGRAM_ID = 77L
        const val UNKNOWN_CODE = 1234
    }
}
