package ru.astrainteractive.messagebridge.messenger.api.discord.model

class DiscordRequestCancelledError(
    cause: Throwable
) : Exception("JDA cancelled the Discord request", cause)
