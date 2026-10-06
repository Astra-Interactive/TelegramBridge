package ru.astrainteractive.messagebridge.messenger.discord.model

internal class DisallowedIntentsError(cause: Throwable) :
    Exception("Discord rejected the requested gateway intents", cause)
