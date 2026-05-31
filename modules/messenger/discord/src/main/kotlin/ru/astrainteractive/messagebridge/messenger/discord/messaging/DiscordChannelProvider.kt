package ru.astrainteractive.messagebridge.messenger.discord.messaging

import club.minnced.discord.webhook.WebhookClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import ru.astrainteractive.klibs.kstorage.api.CachedKrate
import ru.astrainteractive.klibs.kstorage.api.getValue
import ru.astrainteractive.messagebridge.core.PluginConfiguration

internal class DiscordChannelProvider(
    private val jdaFlow: Flow<JDA>,
    private val webHookClientFlow: Flow<WebhookClient>,
    configKrate: CachedKrate<PluginConfiguration>,
) {
    private val config by configKrate

    suspend fun textChannel(): TextChannel? =
        jdaFlow.firstOrNull()?.getTextChannelById(config.jdaConfig.channelId)

    suspend fun webHookClient(): WebhookClient = webHookClientFlow.first()
}
