package ru.astrainteractive.messagebridge.messenger.discord.failure.model

internal class DiscordFailureError(val failure: DiscordFailure) : Exception("$failure")
