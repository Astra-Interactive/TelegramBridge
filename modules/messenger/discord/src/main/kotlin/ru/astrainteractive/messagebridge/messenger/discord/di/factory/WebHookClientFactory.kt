package ru.astrainteractive.messagebridge.messenger.discord.di.factory

import club.minnced.discord.webhook.WebhookClientBuilder
import kotlinx.coroutines.flow.flow
import net.dv8tion.jda.api.JDA
import ru.astrainteractive.astralibs.logging.JUtiltLogger
import ru.astrainteractive.astralibs.logging.Logger
import ru.astrainteractive.messagebridge.messenger.discord.util.RestActionExt.await

internal class WebHookClientFactory(private val jda: JDA) : Logger by JUtiltLogger("WebHookClientFactory") {
    fun create(channelId: String) = flow {
        jda.awaitReady()
        val channel = jda.getTextChannelById(channelId) ?: error("Could not find channel $channelId")
        val webhook = channel.retrieveWebhooks()
            .await()
            .firstOrNull { it.name == "BRIDGE_HOOK_$channelId" }
            ?: channel
                .createWebhook("BRIDGE_HOOK_$channelId")
                .await()
        info { "#create channel: $channelId, url: ${webhook.url}" }
        val client = WebhookClientBuilder(webhook.url)
            .setHttpClient(jda.httpClient)
            .setThreadFactory { job: Runnable? ->
                val thread = Thread(job)
                thread.name = "Thread name"
                thread.isDaemon = true
                thread
            }.setWait(true).build()
        info { "#create WebhookClientBuilder: created" }
        emit(client)
    }
}
