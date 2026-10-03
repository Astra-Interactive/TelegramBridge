package ru.astrainteractive.messagebridge.messenger.discord.api

import net.dv8tion.jda.api.entities.Message

interface DiscordMessageSender {
    suspend fun reply(message: Message, text: String)
}
