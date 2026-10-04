package ru.astrainteractive.messagebridge.messenger.discord.model

class DiscordRequestCancelledError(cause: Throwable) :
    Exception("JDA cancelled the Discord request", cause)
