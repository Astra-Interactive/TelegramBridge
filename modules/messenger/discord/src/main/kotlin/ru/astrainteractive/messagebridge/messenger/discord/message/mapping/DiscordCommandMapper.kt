package ru.astrainteractive.messagebridge.messenger.discord.message.mapping

import ru.astrainteractive.messagebridge.messenger.discord.message.model.DiscordCommand

internal class DiscordCommandMapper {

    fun toCommand(contentRaw: String): DiscordCommand? {
        return when {
            contentRaw == VANILLA -> DiscordCommand.Vanilla
            else -> null
        }
    }

    private companion object {
        const val VANILLA = "!vanilla"
    }
}
