@file:Suppress("KindImportDirection")

package ru.astrainteractive.messagebridge.messenger.discord.connection

import club.minnced.discord.webhook.WebhookClient
import club.minnced.discord.webhook.WebhookClientBuilder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Webhook
import okhttp3.OkHttpClient
import ru.astrainteractive.klibs.mikro.core.logging.JUtiltLogger
import ru.astrainteractive.klibs.mikro.core.logging.Logger
import ru.astrainteractive.messagebridge.messenger.discord.util.await

internal class DiscordWebhookConnector :
    Logger by JUtiltLogger("MessageBridge-DiscordWebhookConnector") {
    private fun buildWebhookClient(webhook: Webhook, httpClient: OkHttpClient): WebhookClient {
        return WebhookClientBuilder(webhook.url)
            .setHttpClient(httpClient)
            .setThreadFactory { job: Runnable? ->
                val thread = Thread(job)
                thread.name = "Thread name"
                thread.isDaemon = true
                thread
            }.setWait(true).build()
    }

    fun connect(jda: JDA, channelId: String): Flow<WebhookClient> {
        return flow {
            jda.awaitReady()
            val channel = jda.getTextChannelById(channelId) ?: error("Could not find channel $channelId")
            val webhook = channel.retrieveWebhooks()
                .await()
                .firstOrNull { existingWebhook -> existingWebhook.name == "BRIDGE_HOOK_$channelId" }
                ?: channel
                    .createWebhook("BRIDGE_HOOK_$channelId")
                    .await()
            verbose { "#connect channel: $channelId, url: ${webhook.url}" }
            val client = buildWebhookClient(webhook, jda.httpClient)
            verbose { "#connect WebhookClientBuilder: created" }
            emit(client)
        }
    }
}
