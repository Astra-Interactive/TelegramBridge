package ru.astrainteractive.messagebridge.messenger.discord.util

import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel

internal fun JDA.findTextChannel(channelId: String): TextChannel? {
    return channelId.toLongOrNull()?.let(::getTextChannelById)
}
