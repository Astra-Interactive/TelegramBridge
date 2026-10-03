@file:Suppress("FunctionNaming")
@file:OptIn(ExperimentalCoroutinesApi::class)

package ru.astrainteractive.messagebridge.link.event

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent
import ru.astrainteractive.klibs.kstorage.api.asCachedKrate
import ru.astrainteractive.klibs.kstorage.api.impl.DefaultMutableKrate
import ru.astrainteractive.messagebridge.core.PluginConfiguration
import ru.astrainteractive.messagebridge.link.api.internal.CodeApiImpl
import ru.astrainteractive.messagebridge.link.api.internal.LinkApiImpl
import ru.astrainteractive.messagebridge.link.controller.DiscordRoleController
import ru.astrainteractive.messagebridge.link.controller.LuckPermsRoleController
import ru.astrainteractive.messagebridge.link.database.model.LinkedPlayerModel
import ru.astrainteractive.messagebridge.link.fake.FakeLinkingDao
import ru.astrainteractive.messagebridge.link.fake.FakeLuckPermsProvider
import ru.astrainteractive.messagebridge.messenger.discord.fake.jdaFake
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class DiscordMemberLeaveListenerTest {
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
    private val linkApi = LinkApiImpl(
        linkingDao = linkingDao,
        codeApi = CodeApiImpl(),
        discordRoleController = DiscordRoleController(configKrate),
        luckPermsRoleController = LuckPermsRoleController(
            configKrate = configKrate,
            luckPermsProvider = luckPermsProvider
        )
    )

    private fun memberLeft(discordId: Long): GuildMemberRemoveEvent {
        return GuildMemberRemoveEvent(
            jdaFake<JDA>(emptyMap()),
            0,
            jdaFake<Guild>(emptyMap()),
            jdaFake<User>(mapOf("getIdLong" to discordId)),
            null
        )
    }

    @Test
    fun GIVEN_linked_player_WHEN_leaves_discord_server_THEN_luckperms_group_removal_is_requested() = runTest {
        linkingDao.upsert(
            LinkedPlayerModel(
                uuid = UUID.fromString("5e4a7f7a-0000-4000-8000-000000000002"),
                lastMinecraftName = "Steve",
                discordLink = LinkedPlayerModel.DiscordLink(lastDiscordName = "Stevie", discordId = DISCORD_ID)
            )
        )
        val listener = DiscordMemberLeaveListener(linkApi = linkApi, ioScope = backgroundScope)

        listener.onGuildMemberRemove(memberLeft(DISCORD_ID))
        runCurrent()

        assertEquals(1, luckPermsProvider.provideCallCount)
    }

    @Test
    fun GIVEN_member_who_never_linked_WHEN_leaves_discord_server_THEN_luckperms_is_not_touched() = runTest {
        val listener = DiscordMemberLeaveListener(linkApi = linkApi, ioScope = backgroundScope)

        listener.onGuildMemberRemove(memberLeft(DISCORD_ID))
        runCurrent()

        assertEquals(0, luckPermsProvider.provideCallCount)
    }

    private companion object {
        const val DISCORD_ID = 4242L
    }
}
