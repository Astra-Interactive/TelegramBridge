@file:Suppress("FunctionNaming")

package ru.astrainteractive.messagebridge.link.event

import kotlinx.coroutines.test.runTest
import ru.astrainteractive.klibs.kstorage.api.asCachedKrate
import ru.astrainteractive.klibs.kstorage.api.impl.DefaultMutableKrate
import ru.astrainteractive.messagebridge.core.config.PluginConfiguration
import ru.astrainteractive.messagebridge.link.dao.fake.FakeLinkingDao
import ru.astrainteractive.messagebridge.link.dao.model.LinkedPlayer
import ru.astrainteractive.messagebridge.link.dao.model.MessengerAccount
import ru.astrainteractive.messagebridge.link.fake.FakeLuckPermsProvider
import ru.astrainteractive.messagebridge.link.internal.LuckPermsRoleController
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LinkedMemberLeaveListenerTest {
    private val configKrate = DefaultMutableKrate(
        factory = {
            PluginConfiguration(
                link = PluginConfiguration.Link(linkDiscordRole = "123456789012345678", linkLuckPermsRole = "verified")
            )
        },
        loader = { null }
    ).asCachedKrate()
    private val luckPermsProvider = FakeLuckPermsProvider()
    private val linkingDao = FakeLinkingDao()
    private val steveUuid = UUID.fromString("5e4a7f7a-0000-4000-8000-000000000002")
    private val stevie = MessengerAccount.Discord(id = DISCORD_ID, name = "Stevie")
    private val steveTelegram = MessengerAccount.Telegram(id = TELEGRAM_ID, username = "steve_tg")
    private val listener = LinkedMemberLeaveListener(
        linkingDao = linkingDao,
        luckPermsRoleController = LuckPermsRoleController(
            configKrate = configKrate,
            luckPermsProvider = luckPermsProvider
        )
    )

    @Test
    fun GIVEN_linked_player_WHEN_leaves_discord_server_THEN_luckperms_group_removal_is_requested() = runTest {
        linkingDao.link(
            uuid = UUID.fromString("5e4a7f7a-0000-4000-8000-000000000002"),
            minecraftName = "Steve",
            account = MessengerAccount.Discord(id = DISCORD_ID, name = "Stevie")
        )

        listener.onMemberLeave(DISCORD_ID)

        assertEquals(1, luckPermsProvider.provideCallCount)
    }

    @Test
    fun GIVEN_player_linked_only_to_discord_WHEN_leaves_discord_server_THEN_the_link_is_removed() = runTest {
        linkingDao.link(uuid = steveUuid, minecraftName = "Steve", account = stevie)

        listener.onMemberLeave(DISCORD_ID)

        assertNull(linkingDao.linkedPlayers[steveUuid])
    }

    @Test
    fun GIVEN_player_linked_to_discord_and_telegram_WHEN_leaves_discord_server_THEN_only_discord_link_is_removed() =
        runTest {
            linkingDao.link(uuid = steveUuid, minecraftName = "Steve", account = stevie)
            linkingDao.link(uuid = steveUuid, minecraftName = "Steve", account = steveTelegram)

            listener.onMemberLeave(DISCORD_ID)

            assertEquals(
                LinkedPlayer(uuid = steveUuid, minecraftName = "Steve", discord = null, telegram = steveTelegram),
                linkingDao.linkedPlayers[steveUuid]
            )
            assertEquals(0, luckPermsProvider.provideCallCount)
        }

    @Test
    fun GIVEN_member_who_never_linked_WHEN_leaves_discord_server_THEN_luckperms_is_not_touched() = runTest {
        listener.onMemberLeave(DISCORD_ID)

        assertEquals(0, luckPermsProvider.provideCallCount)
    }

    private companion object {
        const val DISCORD_ID = 4242L
        const val TELEGRAM_ID = 77L
    }
}
