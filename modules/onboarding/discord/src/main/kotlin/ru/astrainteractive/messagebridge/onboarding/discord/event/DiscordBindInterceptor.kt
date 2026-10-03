package ru.astrainteractive.messagebridge.onboarding.discord.event

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import ru.astrainteractive.messagebridge.messenger.discord.api.DiscordMessageInterceptor
import ru.astrainteractive.messagebridge.onboarding.discord.command.DiscordBindCommandParser
import ru.astrainteractive.messagebridge.onboarding.discord.command.DiscordBindHandler

internal class DiscordBindInterceptor(
    private val scope: CoroutineScope,
    private val commandParser: DiscordBindCommandParser,
    private val bindHandler: DiscordBindHandler,
) : DiscordMessageInterceptor {
    override fun intercept(event: MessageReceivedEvent): Boolean {
        val code = commandParser.map(event.message.contentRaw) ?: return false
        if (!event.isFromGuild || event.isWebhookMessage || event.author.isBot) return false
        scope.launch { bindHandler.bind(code, event) }
        return true
    }
}
