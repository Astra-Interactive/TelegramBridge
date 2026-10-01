package ru.astrainteractive.messagebridge.messenger.discord.di.factory

import club.minnced.discord.webhook.WebhookClient
import club.minnced.discord.webhook.WebhookClientBuilder
import net.dv8tion.jda.api.JDA
import ru.astrainteractive.klibs.mikro.core.logging.JUtiltLogger
import ru.astrainteractive.klibs.mikro.core.logging.Logger
import ru.astrainteractive.messagebridge.messenger.discord.model.DiscordFailure
import ru.astrainteractive.messagebridge.messenger.discord.model.DiscordFailureException
import ru.astrainteractive.messagebridge.messenger.discord.util.RestActionExt.await
import ru.astrainteractive.messagebridge.messenger.discord.util.findTextChannel

internal class WebHookClientFactory(
    private val jda: JDA
) : Logger by JUtiltLogger("WebHookClientFactory") {
    suspend fun create(channelId: String): WebhookClient {
        val channel = jda.findTextChannel(channelId)
            ?: throw DiscordFailureException(DiscordFailure.ChannelNotFound(channelId))
        val webhook = channel.retrieveWebhooks()
            .await()
            .firstOrNull { it.name == "BRIDGE_HOOK_$channelId" }
            ?: channel
                .createWebhook("BRIDGE_HOOK_$channelId")
                .await()
        info { "#create webhook is ready for channel $channelId" }
        val client = WebhookClientBuilder(webhook.url)
            .setHttpClient(jda.httpClient)
            .setThreadFactory { job: Runnable? ->
                val thread = Thread(job)
                thread.name = "Thread name"
                thread.isDaemon = true
                thread
            }.setWait(true).build()
        verbose { "#create WebhookClientBuilder: created" }
        return client
    }
}
