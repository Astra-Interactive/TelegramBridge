package ru.astrainteractive.messagebridge.messenger.discord.channel

import club.minnced.discord.webhook.WebhookClient
import club.minnced.discord.webhook.WebhookClientBuilder
import net.dv8tion.jda.api.JDA
import ru.astrainteractive.klibs.mikro.core.logging.JUtiltLogger
import ru.astrainteractive.klibs.mikro.core.logging.Logger
import ru.astrainteractive.messagebridge.messenger.discord.connection.awaitRequest
import ru.astrainteractive.messagebridge.messenger.discord.failure.DiscordFailure
import ru.astrainteractive.messagebridge.messenger.discord.failure.DiscordFailureError

/** Reuses the webhook the bridge made in the channel before, so a restart does not add one more. */
internal class WebHookClientFactory : Logger by JUtiltLogger("MessageBridge-WebHookClientFactory") {

    suspend fun create(jda: JDA, channelId: String): Result<WebhookClient> {
        val channel = jda.findTextChannel(channelId)
            ?: return Result.failure(DiscordFailureError(DiscordFailure.ChannelNotFound(channelId)))
        val webhookName = "$WEBHOOK_NAME_PREFIX$channelId"
        val webhooks = awaitRequest { channel.retrieveWebhooks() }
            .getOrElse { failure -> return Result.failure(failure) }
        val webhook = webhooks.firstOrNull { webhook -> webhook.name == webhookName }
            ?: awaitRequest { channel.createWebhook(webhookName) }
                .getOrElse { failure -> return Result.failure(failure) }
        info { "#create webhook is ready for channel $channelId" }
        val client = WebhookClientBuilder(webhook.url)
            .setHttpClient(jda.httpClient)
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
