package ru.astrainteractive.messagebridge.messenger.discord.message.model

internal sealed interface DiscordMessageRelevance {
    data object Relevant : DiscordMessageRelevance

    data object PrivateMessage : DiscordMessageRelevance

    data object WebhookMessage : DiscordMessageRelevance
    data object BotAuthor : DiscordMessageRelevance
    data object WrongChannel : DiscordMessageRelevance
}
