package ru.astrainteractive.messagebridge.messenger.discord.connection

import net.dv8tion.jda.api.requests.GatewayIntent
import ru.astrainteractive.messagebridge.core.PluginConfiguration

/**
 * Gateway intents the bot asks for on top of the default ones of JDA. Discord closes the connection with code 4014
 * while a privileged one of them is off in the Developer Portal.
 */
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
        // Only this one tells the bot that a linked player left, which matters only while linking gives roles
        if (config.link != null) add(GatewayIntent.GUILD_MEMBERS)
    }

    /** @return the privileged ones of [intents] by the names the Developer Portal shows */
    fun privilegedNames(intents: Collection<GatewayIntent>): List<String> = intents.mapNotNull(::portalName)
}
