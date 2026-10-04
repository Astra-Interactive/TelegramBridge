@file:Suppress("FunctionNaming")

package ru.astrainteractive.messagebridge.link.event

import kotlinx.coroutines.test.runTest
import ru.astrainteractive.klibs.kstorage.api.asCachedKrate
import ru.astrainteractive.klibs.kstorage.api.impl.DefaultMutableKrate
import ru.astrainteractive.messagebridge.core.PluginConfiguration
import ru.astrainteractive.messagebridge.link.code.internal.CodeApiImpl
import ru.astrainteractive.messagebridge.link.fake.FakeLuckPermsProvider
import ru.astrainteractive.messagebridge.link.internal.DiscordRoleController
import ru.astrainteractive.messagebridge.link.internal.LinkApiImpl
import ru.astrainteractive.messagebridge.link.internal.LuckPermsRoleController
import ru.astrainteractive.messagebridge.link.player.fake.FakeLinkingDao
import ru.astrainteractive.messagebridge.link.player.model.LinkedPlayerModel
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

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
    private val listener = LinkedMemberLeaveListener(
        linkApi = LinkApiImpl(
            linkingDao = linkingDao,
            codeApi = CodeApiImpl(),
            discordRoleController = DiscordRoleController(configKrate),
            luckPermsRoleController = LuckPermsRoleController(
                configKrate = configKrate,
                luckPermsProvider = luckPermsProvider
            )
        )
    )

    @Test
    fun GIVEN_linked_player_WHEN_leaves_discord_server_THEN_luckperms_group_removal_is_requested() = runTest {
        linkingDao.upsert(
            LinkedPlayerModel(
                uuid = UUID.fromString("5e4a7f7a-0000-4000-8000-000000000002"),
                lastMinecraftName = "Steve",
                discordLink = LinkedPlayerModel.DiscordLink(lastDiscordName = "Stevie", discordId = DISCORD_ID)
            )
        )

        listener.onMemberLeave(DISCORD_ID)

        assertEquals(1, luckPermsProvider.provideCallCount)
    }

    @Test
    fun GIVEN_member_who_never_linked_WHEN_leaves_discord_server_THEN_luckperms_is_not_touched() = runTest {
        listener.onMemberLeave(DISCORD_ID)

        assertEquals(0, luckPermsProvider.provideCallCount)
    }

    private companion object {
        const val DISCORD_ID = 4242L
    }
}
