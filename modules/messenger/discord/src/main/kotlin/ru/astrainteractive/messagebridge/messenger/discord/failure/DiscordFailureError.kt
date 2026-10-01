package ru.astrainteractive.messagebridge.messenger.discord.failure

/** A [DiscordFailure] the bridge finds itself, before Discord could report it, carried by a failed `Result`. */
internal class DiscordFailureError(val failure: DiscordFailure) : Exception("$failure")
