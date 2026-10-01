package ru.astrainteractive.messagebridge.messenger.discord.impl.relay.model

internal sealed interface DiscordMessageRelevance {
    /** Guild message in the configured bridge channel — handle as a command or relay it. */
    data object Relevant : DiscordMessageRelevance

    data object WebhookMessage : DiscordMessageRelevance
    data object BotAuthor : DiscordMessageRelevance
    data object WrongChannel : DiscordMessageRelevance
}
