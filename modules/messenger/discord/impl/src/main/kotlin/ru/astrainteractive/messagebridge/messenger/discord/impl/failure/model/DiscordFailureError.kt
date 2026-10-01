package ru.astrainteractive.messagebridge.messenger.discord.impl.failure.model

import ru.astrainteractive.messagebridge.messenger.discord.api.model.DiscordFailure

internal class DiscordFailureError(val failure: DiscordFailure) : Exception("$failure")
