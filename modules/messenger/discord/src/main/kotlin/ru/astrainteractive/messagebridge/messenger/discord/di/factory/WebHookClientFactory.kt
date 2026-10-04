package ru.astrainteractive.messagebridge.messenger.discord.di.factory

import club.minnced.discord.webhook.WebhookClientBuilder
import kotlinx.coroutines.flow.flow
import net.dv8tion.jda.api.JDA
import ru.astrainteractive.klibs.mikro.core.logging.JUtiltLogger
import ru.astrainteractive.klibs.mikro.core.logging.Logger
import ru.astrainteractive.messagebridge.messenger.discord.util.RestActionExt.await

internal class WebHookClientFactory :
    Logger by JUtiltLogger("MessageBridge-WebHookClientFactory").withoutParentHandlers() {
    fun create(jda: JDA, channelId: String) = flow {
        jda.awaitReady()
        val channel = jda.getTextChannelById(channelId) ?: error("Could not find channel $channelId")
        val webhook = channel.retrieveWebhooks()
            .await()
            .firstOrNull { existingWebhook -> existingWebhook.name == "BRIDGE_HOOK_$channelId" }
            ?: channel
                .createWebhook("BRIDGE_HOOK_$channelId")
                .await()
        verbose { "#create channel: $channelId, url: ${webhook.url}" }
        val client = WebhookClientBuilder(webhook.url)
            .setHttpClient(jda.httpClient)
            .setThreadFactory { job: Runnable? ->
                val thread = Thread(job)
                thread.name = "Thread name"
                thread.isDaemon = true
                thread
            }.setWait(true).build()
        verbose { "#create WebhookClientBuilder: created" }
        emit(client)
    }
}
