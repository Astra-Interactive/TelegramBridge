package ru.astrainteractive.messagebridge.link.discord.event

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import net.dv8tion.jda.api.entities.channel.ChannelType
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import ru.astrainteractive.messagebridge.core.api.config.PluginConfiguration
import ru.astrainteractive.messagebridge.link.discord.command.DiscordLinkHandler
import ru.astrainteractive.messagebridge.messenger.discord.api.api.DiscordMessageInterceptor

internal class DiscordLinkInterceptor(
    private val scope: CoroutineScope,
    private val configFlow: StateFlow<PluginConfiguration>,
    private val linkHandler: DiscordLinkHandler,
) : DiscordMessageInterceptor {
    private fun MessageReceivedEvent.isLinkInBridgeChannel(): Boolean {
        return message.channelId == configFlow.value.jdaConfig.channelId && message.contentRaw.startsWith(LINK)
    }

    override fun intercept(event: MessageReceivedEvent): Boolean {
        if (event.isWebhookMessage || event.author.isBot) return false
        val content = event.message.contentRaw
        return when {
            event.channelType == ChannelType.PRIVATE -> {
                val code = content.toIntOrNull() ?: INVALID_CODE
                scope.launch { linkHandler.linkFromPrivate(code, event) }
                true
            }

            event.isLinkInBridgeChannel() -> {
                val code = content.replace("$LINK ", "").toIntOrNull() ?: INVALID_CODE
                scope.launch { linkHandler.linkInChannel(code, event) }
                true
            }

            else -> false
        }
    }

    private companion object {
        const val LINK = "/link"
        const val INVALID_CODE = -1
    }
}
