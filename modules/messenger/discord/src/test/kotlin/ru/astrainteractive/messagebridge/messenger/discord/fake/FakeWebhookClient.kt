package ru.astrainteractive.messagebridge.messenger.discord.fake

import club.minnced.discord.webhook.WebhookClient
import club.minnced.discord.webhook.receive.ReadonlyMessage
import club.minnced.discord.webhook.receive.ReadonlyUser
import club.minnced.discord.webhook.send.AllowedMentions
import club.minnced.discord.webhook.send.WebhookMessage
import okhttp3.OkHttpClient
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors

internal class FakeWebhookClient : WebhookClient(
    0L,
    "token",
    true,
    OkHttpClient(),
    Executors.newSingleThreadScheduledExecutor(),
    AllowedMentions.all(),
    0L
) {
    val sent = mutableListOf<WebhookMessage>()
    var failure: Throwable? = null

    private fun sentMessage(id: Long): ReadonlyMessage = ReadonlyMessage(
        id,
        0L,
        false,
        false,
        0,
        ReadonlyUser(0L, 0, true, "bridge", null),
        "",
        emptyList(),
        emptyList(),
        emptyList(),
        emptyList()
    )

    override fun send(message: WebhookMessage): CompletableFuture<ReadonlyMessage> {
        sent += message
        val t = failure ?: return CompletableFuture.completedFuture(sentMessage(sent.size.toLong()))
        return CompletableFuture.failedFuture(t)
    }
}
