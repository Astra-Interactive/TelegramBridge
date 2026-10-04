@file:Suppress("FunctionNaming")

package ru.astrainteractive.messagebridge.link.internal

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.test.runTest
import ru.astrainteractive.klibs.kstorage.api.asCachedKrate
import ru.astrainteractive.klibs.kstorage.api.impl.DefaultMutableKrate
import ru.astrainteractive.messagebridge.core.config.PluginConfiguration
import ru.astrainteractive.messagebridge.messenger.discord.model.DiscordRoleChange
import kotlin.test.Test
import kotlin.test.assertTrue

class DiscordRoleControllerTest {
    private val roleChanges = Channel<DiscordRoleChange>(Channel.UNLIMITED)

    private fun controller(link: PluginConfiguration.Link?): DiscordRoleController {
        val configKrate = DefaultMutableKrate(
            factory = { PluginConfiguration(link = link) },
            loader = { null }
        ).asCachedKrate()
        return DiscordRoleController(configKrate = configKrate, roleChanges = roleChanges)
    }

    @Test
    fun GIVEN_config_without_link_block_WHEN_linked_role_is_added_THEN_no_role_change_is_sent() = runTest {
        controller(link = null).addLinkedRole(DISCORD_ID)

        assertTrue(roleChanges.tryReceive().isFailure)
    }

    @Test
    fun GIVEN_link_role_that_is_not_a_snowflake_WHEN_linked_role_is_added_THEN_no_role_change_is_sent() = runTest {
        val link = PluginConfiguration.Link(linkDiscordRole = "verified", linkLuckPermsRole = "verified")

        controller(link = link).addLinkedRole(DISCORD_ID)

        assertTrue(roleChanges.tryReceive().isFailure)
    }

    @Test
    fun GIVEN_config_without_link_block_WHEN_linked_role_is_removed_THEN_no_role_change_is_sent() = runTest {
        controller(link = null).removeLinkedRole(DISCORD_ID)

        assertTrue(roleChanges.tryReceive().isFailure)
    }

    @Test
    fun GIVEN_link_role_that_is_not_a_snowflake_WHEN_linked_role_is_removed_THEN_no_role_change_is_sent() = runTest {
        val link = PluginConfiguration.Link(linkDiscordRole = "verified", linkLuckPermsRole = "verified")

        controller(link = link).removeLinkedRole(DISCORD_ID)

        assertTrue(roleChanges.tryReceive().isFailure)
    }

    private companion object {
        const val DISCORD_ID = 4242L
    }
}
