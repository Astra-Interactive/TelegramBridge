package ru.astrainteractive.messagebridge.messenger.discord.connection.internal

import net.dv8tion.jda.api.requests.GatewayIntent
import ru.astrainteractive.messagebridge.core.api.config.PluginConfiguration

internal object DiscordIntents {

    private fun portalName(intent: GatewayIntent): String? = when (intent) {
        GatewayIntent.MESSAGE_CONTENT -> "Message Content Intent"
        GatewayIntent.GUILD_MEMBERS -> "Server Members Intent"
        GatewayIntent.GUILD_PRESENCES -> "Presence Intent"
        else -> null
    }

    fun requiredBy(config: PluginConfiguration): Set<GatewayIntent> = buildSet {
        add(GatewayIntent.MESSAGE_CONTENT)
        add(GatewayIntent.GUILD_MESSAGES)
        add(GatewayIntent.DIRECT_MESSAGES)
        if (config.link != null) add(GatewayIntent.GUILD_MEMBERS)
    }

    fun privilegedNames(intents: Collection<GatewayIntent>): List<String> = intents.mapNotNull(::portalName)
}
