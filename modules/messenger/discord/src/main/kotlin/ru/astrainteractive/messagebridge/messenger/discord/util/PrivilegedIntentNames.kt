package ru.astrainteractive.messagebridge.messenger.discord.util

import net.dv8tion.jda.api.requests.GatewayIntent

private fun GatewayIntent.portalName(): String? = when (this) {
    GatewayIntent.MESSAGE_CONTENT -> "Message Content Intent"
    GatewayIntent.GUILD_MEMBERS -> "Server Members Intent"
    GatewayIntent.GUILD_PRESENCES -> "Presence Intent"
    else -> null
}

fun Collection<GatewayIntent>.privilegedNames(): List<String> = mapNotNull { intent -> intent.portalName() }
