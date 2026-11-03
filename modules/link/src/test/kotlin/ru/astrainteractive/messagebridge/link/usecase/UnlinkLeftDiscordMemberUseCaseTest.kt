@file:Suppress("FunctionNaming", "ArgumentPerLine", "ExpressionBodyOnOneLine")

package ru.astrainteractive.messagebridge.link.usecase

import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.runTest
import ru.astrainteractive.messagebridge.link.fake.FakeLinkRoleRepository
import ru.astrainteractive.messagebridge.link.fake.FakeLinkedPlayerRepository
import ru.astrainteractive.messagebridge.link.model.LinkedPlayer
import ru.astrainteractive.messagebridge.link.model.MessengerAccount
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class UnlinkLeftDiscordMemberUseCaseTest {
    private val linkRoleRepository = FakeLinkRoleRepository()
    private val linkedPlayerRepository = FakeLinkedPlayerRepository()
    private val steveUuid = UUID.fromString("5e4a7f7a-0000-4000-8000-000000000002")
    private val stevie = MessengerAccount.Discord(id = DISCORD_ID, name = "Stevie")
    private val steveTelegram = MessengerAccount.Telegram(id = TELEGRAM_ID, username = "steve_tg")
    private val unlinkLeftDiscordMember = UnlinkLeftDiscordMemberUseCase(
        linkedPlayerRepository = linkedPlayerRepository,
        linkRoleRepository = linkRoleRepository
    )

    @Test
    fun GIVEN_linked_player_WHEN_leaves_discord_server_THEN_luckperms_group_removal_is_requested(): TestResult {
        return runTest {
            linkedPlayerRepository.link(
                uuid = UUID.fromString("5e4a7f7a-0000-4000-8000-000000000002"),
                minecraftName = "Steve",
                account = MessengerAccount.Discord(id = DISCORD_ID, name = "Stevie")
            )

            unlinkLeftDiscordMember.invoke(DISCORD_ID)

            assertEquals(listOf(steveUuid), linkRoleRepository.removed)
        }
    }

    @Test
    fun GIVEN_player_linked_only_to_discord_WHEN_leaves_discord_server_THEN_the_link_is_removed(): TestResult {
        return runTest {
            linkedPlayerRepository.link(uuid = steveUuid, minecraftName = "Steve", account = stevie)

            unlinkLeftDiscordMember.invoke(DISCORD_ID)

            assertNull(linkedPlayerRepository.linkedPlayers[steveUuid])
        }
    }

    @Test
    fun GIVEN_player_linked_to_discord_and_telegram_WHEN_leaves_discord_THEN_only_discord_is_unlinked(): TestResult {
        return runTest {
            linkedPlayerRepository.link(uuid = steveUuid, minecraftName = "Steve", account = stevie)
            linkedPlayerRepository.link(uuid = steveUuid, minecraftName = "Steve", account = steveTelegram)

            unlinkLeftDiscordMember.invoke(DISCORD_ID)

            assertEquals(
                LinkedPlayer(uuid = steveUuid, minecraftName = "Steve", discord = null, telegram = steveTelegram),
                linkedPlayerRepository.linkedPlayers[steveUuid]
            )
            assertTrue(linkRoleRepository.removed.isEmpty())
        }
    }

    @Test
    fun GIVEN_member_who_never_linked_WHEN_leaves_discord_server_THEN_luckperms_is_not_touched(): TestResult {
        return runTest {
            unlinkLeftDiscordMember.invoke(DISCORD_ID)

            assertTrue(linkRoleRepository.removed.isEmpty())
        }
    }

    private companion object {
        const val DISCORD_ID = 4242L
        const val TELEGRAM_ID = 77L
    }
}
