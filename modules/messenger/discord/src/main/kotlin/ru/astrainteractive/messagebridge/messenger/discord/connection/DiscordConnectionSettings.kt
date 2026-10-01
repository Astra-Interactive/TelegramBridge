package ru.astrainteractive.messagebridge.messenger.discord.connection

import net.dv8tion.jda.api.requests.GatewayIntent
import ru.astrainteractive.messagebridge.core.PluginConfiguration

/** What the bot connects with; a change of any of it connects the bot again. */
internal data class DiscordConnectionSettings(
    val jdaConfig: PluginConfiguration.JdaConfig,
    val intents: Set<GatewayIntent>
)
