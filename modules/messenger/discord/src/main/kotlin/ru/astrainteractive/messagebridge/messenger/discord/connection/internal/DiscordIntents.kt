package ru.astrainteractive.messagebridge.messenger.discord.connection.internal

import net.dv8tion.jda.api.requests.GatewayIntent
import ru.astrainteractive.messagebridge.core.api.config.PluginConfiguration

internal object DiscordIntents {

    fun requiredBy(config: PluginConfiguration): Set<GatewayIntent> = buildSet {
        add(GatewayIntent.MESSAGE_CONTENT)
        add(GatewayIntent.GUILD_MESSAGES)
        add(GatewayIntent.DIRECT_MESSAGES)
        if (config.link != null) add(GatewayIntent.GUILD_MEMBERS)
    }
}
