@file:Suppress("FunctionNaming")

package ru.astrainteractive.messagebridge.messenger.discord.connection.internal

import net.dv8tion.jda.api.requests.GatewayIntent
import ru.astrainteractive.messagebridge.core.api.config.PluginConfiguration
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DiscordIntentsTest {
    private val link = PluginConfiguration.Link(linkDiscordRole = "1", linkLuckPermsRole = "verified")

    @Test
    fun GIVEN_linking_gives_roles_WHEN_intents_THEN_the_bot_asks_for_the_members_of_the_server() {
        val intents = DiscordIntents.requiredBy(PluginConfiguration(link = link))

        assertTrue(GatewayIntent.GUILD_MEMBERS in intents)
        assertEquals(
            listOf("Message Content Intent", "Server Members Intent"),
            DiscordIntents.privilegedNames(intents)
        )
    }

    @Test
    fun GIVEN_linking_gives_no_roles_WHEN_intents_THEN_only_message_content_has_to_be_turned_on() {
        val intents = DiscordIntents.requiredBy(PluginConfiguration(link = null))

        assertFalse(GatewayIntent.GUILD_MEMBERS in intents)
        assertTrue(GatewayIntent.MESSAGE_CONTENT in intents)
        assertEquals(listOf("Message Content Intent"), DiscordIntents.privilegedNames(intents))
    }

    @Test
    fun GIVEN_intents_that_need_no_switch_WHEN_named_THEN_they_are_left_out() {
        val names = DiscordIntents.privilegedNames(listOf(GatewayIntent.GUILD_MESSAGES, GatewayIntent.GUILD_PRESENCES))

        assertEquals(listOf("Presence Intent"), names)
    }
}
