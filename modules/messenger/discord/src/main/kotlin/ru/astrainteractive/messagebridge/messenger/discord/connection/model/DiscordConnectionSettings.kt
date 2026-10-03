package ru.astrainteractive.messagebridge.messenger.discord.connection.model

import net.dv8tion.jda.api.requests.GatewayIntent
import ru.astrainteractive.messagebridge.core.api.config.PluginConfiguration

internal data class DiscordConnectionSettings(
    val jdaConfig: PluginConfiguration.JdaConfig,
    val intents: Set<GatewayIntent>
)
