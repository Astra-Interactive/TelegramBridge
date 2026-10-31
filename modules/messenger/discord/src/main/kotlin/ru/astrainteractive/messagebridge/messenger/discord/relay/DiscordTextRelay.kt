package ru.astrainteractive.messagebridge.messenger.discord.relay

import club.minnced.discord.webhook.WebhookClient
import kotlinx.coroutines.future.await
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import ru.astrainteractive.messagebridge.messenger.api.model.MessageRef
import ru.astrainteractive.messagebridge.messenger.api.model.Text
import ru.astrainteractive.messagebridge.messenger.discord.mapping.DiscordWebhookMessageMapper

internal class DiscordTextRelay(
    private val memberResolver: DiscordMemberResolver,
    private val webhookMessageMapper: DiscordWebhookMessageMapper,
    private val relayedMessageCache: DiscordRelayedMessageCache,
) {
    private suspend fun replyJumpUrl(reply: Text.Reply, channel: TextChannel): String? {
        val target = reply.target ?: return null
        val messageId = if (target is MessageRef.Discord) {
            target.messageId
        } else {
            relayedMessageCache.copyOf(target) ?: return null
        }
        return "${channel.jumpUrl}/$messageId"
    }

    suspend fun relay(event: Text, channel: TextChannel, webhookClient: WebhookClient) {
        val member = memberResolver.resolve(channel, event)
        val replyJumpUrl = event.reply?.let { reply -> replyJumpUrl(reply, channel) }
        val message = webhookMessageMapper.toWebhookMessage(
            event,
            member,
            replyJumpUrl
        )
        val sentMessage = webhookClient.send(message).await()
        relayedMessageCache.remember(sentMessage.id, event)
    }
}
