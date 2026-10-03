package ru.astrainteractive.messagebridge.messenger.discord.channel.network

import club.minnced.discord.webhook.WebhookClient
import club.minnced.discord.webhook.WebhookClientBuilder
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import ru.astrainteractive.klibs.mikro.core.logging.JUtiltLogger
import ru.astrainteractive.klibs.mikro.core.logging.Logger
import ru.astrainteractive.messagebridge.messenger.discord.util.awaitRequest

internal class WebHookClientFactory : Logger by JUtiltLogger("MessageBridge-WebHookClientFactory") {

    suspend fun create(channel: TextChannel): Result<WebhookClient> {
        val webhookName = "$WEBHOOK_NAME_PREFIX${channel.id}"
        val webhooks = awaitRequest { channel.retrieveWebhooks() }
            .getOrElse { t -> return Result.failure(t) }
        val webhook = webhooks.firstOrNull { webhook -> webhook.name == webhookName }
            ?: awaitRequest { channel.createWebhook(webhookName) }
                .getOrElse { t -> return Result.failure(t) }
        info { "#create webhook is ready for channel ${channel.id}" }
        val client = WebhookClientBuilder(webhook.url)
            .setHttpClient(channel.jda.httpClient)
            .setThreadFactory { job ->
                val thread = Thread(job)
                thread.name = THREAD_NAME
                thread.isDaemon = true
                thread
            }
            .setWait(true)
            .build()
        return Result.success(client)
    }

    private companion object {
        const val WEBHOOK_NAME_PREFIX = "BRIDGE_HOOK_"
        const val THREAD_NAME = "MessageBridge-DiscordWebhook"
    }
}
