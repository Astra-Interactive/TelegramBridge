@file:Suppress("FunctionNaming")

package ru.astrainteractive.messagebridge.messenger.discord.api.util

import net.dv8tion.jda.api.requests.GatewayIntent
import kotlin.test.Test
import kotlin.test.assertEquals

class PrivilegedIntentNamesTest {
    @Test
    fun GIVEN_privileged_intents_WHEN_named_THEN_they_have_the_names_of_the_developer_portal() {
        val names = listOf(GatewayIntent.MESSAGE_CONTENT, GatewayIntent.GUILD_MEMBERS).privilegedNames()

        assertEquals(listOf("Message Content Intent", "Server Members Intent"), names)
    }

    @Test
    fun GIVEN_intents_that_need_no_switch_WHEN_named_THEN_they_are_left_out() {
        val names = listOf(GatewayIntent.GUILD_MESSAGES, GatewayIntent.GUILD_PRESENCES).privilegedNames()

        assertEquals(listOf("Presence Intent"), names)
    }
}
