package ru.astrainteractive.messagebridge.messenger.discord.api

import net.dv8tion.jda.api.events.message.MessageReceivedEvent

fun interface DiscordMessageInterceptor {
    fun intercept(event: MessageReceivedEvent): Boolean
}
