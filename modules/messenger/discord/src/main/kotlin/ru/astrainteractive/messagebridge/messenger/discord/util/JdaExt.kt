package ru.astrainteractive.messagebridge.messenger.discord.util

import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel

/** JDA throws on an id that is not a number, and `channel_id` is typed by hand. */
internal fun JDA.findTextChannel(channelId: String): TextChannel? {
    return channelId.toLongOrNull()?.let(::getTextChannelById)
}
