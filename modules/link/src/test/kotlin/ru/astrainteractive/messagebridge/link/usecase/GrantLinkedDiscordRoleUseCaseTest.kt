@file:Suppress("FunctionNaming")

package ru.astrainteractive.messagebridge.link.usecase

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.test.runTest
import ru.astrainteractive.klibs.kstorage.api.asCachedKrate
import ru.astrainteractive.klibs.kstorage.api.impl.DefaultMutableKrate
import ru.astrainteractive.messagebridge.core.api.config.model.PluginConfiguration
import ru.astrainteractive.messagebridge.messenger.api.discord.model.DiscordRoleChange
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GrantLinkedDiscordRoleUseCaseTest {
    private val roleChanges = Channel<DiscordRoleChange>(Channel.UNLIMITED)

    private fun useCase(link: PluginConfiguration.Link?): GrantLinkedDiscordRoleUseCase {
        val configKrate = DefaultMutableKrate(
            factory = { PluginConfiguration(link = link) },
            loader = { null }
        ).asCachedKrate()
        return GrantLinkedDiscordRoleUseCase(configKrate = configKrate, roleChanges = roleChanges)
    }

    @Test
    fun GIVEN_config_without_link_block_WHEN_role_is_granted_THEN_no_role_change_is_sent() = runTest {
        useCase(link = null).invoke(DISCORD_ID)

        assertTrue(roleChanges.tryReceive().isFailure)
    }

    @Test
    fun GIVEN_link_role_that_is_not_a_snowflake_WHEN_role_is_granted_THEN_no_role_change_is_sent() = runTest {
        val link = PluginConfiguration.Link(linkDiscordRole = "verified", linkLuckPermsRole = "verified")

        useCase(link = link).invoke(DISCORD_ID)

        assertTrue(roleChanges.tryReceive().isFailure)
    }

    @Test
    fun GIVEN_link_role_snowflake_WHEN_role_is_granted_THEN_grant_is_sent_for_that_role() = runTest {
        val link = PluginConfiguration.Link(linkDiscordRole = ROLE_ID.toString(), linkLuckPermsRole = "verified")

        useCase(link = link).invoke(DISCORD_ID)

        assertEquals(
            DiscordRoleChange.Grant(discordUserId = DISCORD_ID, roleId = ROLE_ID),
            roleChanges.tryReceive().getOrNull()
        )
    }

    private companion object {
        const val DISCORD_ID = 4242L
        const val ROLE_ID = 123456789012345678L
    }
}
