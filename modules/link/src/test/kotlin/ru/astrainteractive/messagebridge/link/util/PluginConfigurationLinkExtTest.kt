@file:Suppress("FunctionNaming")

package ru.astrainteractive.messagebridge.link.util

import ru.astrainteractive.messagebridge.core.api.config.model.PluginConfiguration
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PluginConfigurationLinkExtTest {
    @Test
    fun GIVEN_numeric_link_role_WHEN_role_id_is_read_THEN_it_is_the_snowflake() {
        val link = PluginConfiguration.Link(linkDiscordRole = "123456789012345678", linkLuckPermsRole = "verified")

        assertEquals(123456789012345678L, link.discordRoleIdOrNull())
    }

    @Test
    fun GIVEN_link_role_that_is_not_a_snowflake_WHEN_role_id_is_read_THEN_it_is_null() {
        val link = PluginConfiguration.Link(linkDiscordRole = "verified", linkLuckPermsRole = "verified")

        assertNull(link.discordRoleIdOrNull())
    }
}
