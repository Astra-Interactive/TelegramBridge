@file:Suppress("FunctionNaming")

package ru.astrainteractive.messagebridge.link.usecase

import kotlinx.coroutines.test.runTest
import ru.astrainteractive.messagebridge.link.code.memory.InMemoryLinkCodeRepository
import ru.astrainteractive.messagebridge.link.code.model.CodeUser
import ru.astrainteractive.messagebridge.link.fake.FakeLinkRoleRepository
import ru.astrainteractive.messagebridge.link.fake.FakeLinkedPlayerRepository
import ru.astrainteractive.messagebridge.link.model.LinkResponse
import ru.astrainteractive.messagebridge.link.model.LinkedPlayer
import ru.astrainteractive.messagebridge.link.model.MessengerAccount
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LinkAccountUseCaseTest {
    private val linkCodeRepository = InMemoryLinkCodeRepository()
    private val linkedPlayerRepository = FakeLinkedPlayerRepository()
    private val linkRoleRepository = FakeLinkRoleRepository()
    private val useCase = LinkAccountUseCase(
        linkCodeRepository = linkCodeRepository,
        linkedPlayerRepository = linkedPlayerRepository,
        linkRoleRepository = linkRoleRepository
    )
    private val steve = CodeUser(name = "Steve", uuid = UUID.fromString("5e4a7f7a-0000-4000-8000-000000000002"))
    private val stevie = MessengerAccount.Discord(id = DISCORD_ID, name = "Stevie")
    private val steveTelegram = MessengerAccount.Telegram(id = TELEGRAM_ID, username = "steve_tg")
    private val alexUuid = UUID.fromString("5e4a7f7a-0000-4000-8000-000000000003")

    @Test
    fun GIVEN_code_of_a_player_WHEN_discord_account_sends_it_THEN_account_is_linked_to_that_player() = runTest {
        val code = linkCodeRepository.generateCodeForPlayer(steve)

        val response = useCase(code, stevie)

        assertEquals(LinkResponse.Linked, response)
        assertEquals(
            LinkedPlayer(uuid = steve.uuid, minecraftName = "Steve", discord = stevie, telegram = null),
            linkedPlayerRepository.linkedPlayers[steve.uuid]
        )
    }

    @Test
    fun GIVEN_player_linked_to_discord_WHEN_telegram_account_sends_a_new_code_THEN_both_accounts_stay_linked() =
        runTest {
            useCase(linkCodeRepository.generateCodeForPlayer(steve), stevie)

            val response = useCase(linkCodeRepository.generateCodeForPlayer(steve), steveTelegram)

            assertEquals(LinkResponse.Linked, response)
            assertEquals(
                LinkedPlayer(uuid = steve.uuid, minecraftName = "Steve", discord = stevie, telegram = steveTelegram),
                linkedPlayerRepository.linkedPlayers[steve.uuid]
            )
        }

    @Test
    fun GIVEN_account_linked_WHEN_link_succeeds_THEN_luckperms_group_is_requested() = runTest {
        useCase(linkCodeRepository.generateCodeForPlayer(steve), stevie)

        assertEquals(listOf(steve.uuid), linkRoleRepository.added)
    }

    @Test
    fun GIVEN_code_nobody_created_WHEN_account_sends_it_THEN_no_code_and_nothing_is_linked() = runTest {
        val response = useCase(UNKNOWN_CODE, stevie)

        assertEquals(LinkResponse.NoCode, response)
        assertTrue(linkedPlayerRepository.linkedPlayers.isEmpty())
        assertTrue(linkRoleRepository.added.isEmpty())
    }

    @Test
    fun GIVEN_player_already_linked_to_discord_WHEN_another_discord_account_sends_code_THEN_already_linked() =
        runTest {
            val existing = MessengerAccount.Discord(id = OTHER_DISCORD_ID, name = "Old")
            linkedPlayerRepository.link(uuid = steve.uuid, minecraftName = "Steve", account = existing)
            val code = linkCodeRepository.generateCodeForPlayer(steve)

            val response = useCase(code, stevie)

            assertEquals(LinkResponse.AlreadyLinked, response)
            assertEquals(existing, linkedPlayerRepository.linkedPlayers[steve.uuid]?.discord)
        }

    @Test
    fun GIVEN_database_that_can_not_write_WHEN_account_sends_code_THEN_unknown_error_and_no_group_is_requested() =
        runTest {
            val code = linkCodeRepository.generateCodeForPlayer(steve)
            linkedPlayerRepository.linkFailure = IllegalStateException("Database is read-only")

            val response = useCase(code, stevie)

            assertEquals(LinkResponse.UnknownError, response)
            assertTrue(linkedPlayerRepository.linkedPlayers.isEmpty())
            assertTrue(linkRoleRepository.added.isEmpty())
        }

    @Test
    fun GIVEN_discord_account_linked_to_another_player_WHEN_it_sends_a_code_THEN_account_taken_and_links_stay() =
        runTest {
            linkedPlayerRepository.link(uuid = alexUuid, minecraftName = "Alex", account = stevie)
            val code = linkCodeRepository.generateCodeForPlayer(steve)

            val response = useCase(code, stevie)

            assertEquals(LinkResponse.AccountTaken, response)
            assertEquals(null, linkedPlayerRepository.linkedPlayers[steve.uuid])
            assertEquals(stevie, linkedPlayerRepository.linkedPlayers[alexUuid]?.discord)
        }

    @Test
    fun GIVEN_telegram_account_linked_to_another_player_WHEN_it_sends_a_code_THEN_account_taken() = runTest {
        linkedPlayerRepository.link(uuid = alexUuid, minecraftName = "Alex", account = steveTelegram)
        val code = linkCodeRepository.generateCodeForPlayer(steve)

        val response = useCase(code, steveTelegram)

        assertEquals(LinkResponse.AccountTaken, response)
        assertTrue(linkRoleRepository.added.isEmpty())
    }

    @Test
    fun GIVEN_account_linked_WHEN_the_same_code_is_sent_again_THEN_no_code() = runTest {
        val code = linkCodeRepository.generateCodeForPlayer(steve)
        useCase(code, stevie)

        assertEquals(LinkResponse.NoCode, useCase(code, steveTelegram))
    }

    @Test
    fun GIVEN_database_that_can_not_write_WHEN_code_is_sent_THEN_the_code_still_links_later() = runTest {
        val code = linkCodeRepository.generateCodeForPlayer(steve)
        linkedPlayerRepository.linkFailure = IllegalStateException("Database is read-only")
        useCase(code, stevie)
        linkedPlayerRepository.linkFailure = null

        assertEquals(LinkResponse.Linked, useCase(code, stevie))
    }

    @Test
    fun GIVEN_account_linked_to_another_player_WHEN_it_sends_a_code_THEN_the_code_stays_for_another_account() =
        runTest {
            linkedPlayerRepository.link(uuid = alexUuid, minecraftName = "Alex", account = stevie)
            val code = linkCodeRepository.generateCodeForPlayer(steve)
            useCase(code, stevie)

            assertEquals(LinkResponse.Linked, useCase(code, steveTelegram))
        }

    private companion object {
        const val DISCORD_ID = 4242L
        const val OTHER_DISCORD_ID = 4343L
        const val TELEGRAM_ID = 77L
        const val UNKNOWN_CODE = 1234
    }
}
