package ru.astrainteractive.messagebridge.discord.event.di.factory

import club.minnced.discord.webhook.WebhookClientBuilder
import kotlinx.coroutines.flow.flow
import net.dv8tion.jda.api.JDA
import ru.astrainteractive.messagebridge.discord.util.RestActionExt.await

class WebHookClientFactory(private val jda: JDA) {
    fun create(channelId: String) = flow {
        val channel = jda.getTextChannelById(channelId) ?: error("Could not find channel")
        val webhook = channel.retrieveWebhooks()
            .await()
            .firstOrNull { it.name == "BRIDGE_HOOK" }
            ?: channel
                .createWebhook("BRIDGE_HOOK")
                .await()
        val client = WebhookClientBuilder(webhook.url).setThreadFactory { job: Runnable? ->
            val thread = Thread(job)
            thread.name = "Thread name"
            thread.isDaemon = true
            thread
        }.setWait(true).build()
        emit(client)
    }
}
