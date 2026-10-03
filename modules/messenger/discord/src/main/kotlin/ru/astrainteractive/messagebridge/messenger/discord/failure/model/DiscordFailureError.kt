package ru.astrainteractive.messagebridge.messenger.discord.failure.model

import ru.astrainteractive.messagebridge.messenger.discord.model.DiscordFailure

internal class DiscordFailureError(val failure: DiscordFailure) : Exception("$failure")
