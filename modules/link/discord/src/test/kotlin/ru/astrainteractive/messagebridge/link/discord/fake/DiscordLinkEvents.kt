package ru.astrainteractive.messagebridge.link.discord.fake

import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.entities.channel.ChannelType
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import ru.astrainteractive.messagebridge.messenger.discord.fake.jdaFake

internal const val BRIDGE_CHANNEL_ID = 10L
internal const val MEMBER_ID = 42L
internal const val MEMBER_NAME = "steve"

internal fun memberOf(guild: Guild? = null): Member = jdaFake { method, _ ->
    when (method.name) {
        "getIdLong" -> MEMBER_ID
        "getId" -> "$MEMBER_ID"
        "getEffectiveName" -> MEMBER_NAME
        "getGuild" -> guild
        else -> null
    }
}

internal fun messageEventOf(
    text: String,
    channelId: Long = BRIDGE_CHANNEL_ID,
    channelType: ChannelType = ChannelType.TEXT,
    member: Member? = memberOf(),
    jda: JDA = jdaFake { _, _ -> null },
    isWebhookMessage: Boolean = false,
    isBot: Boolean = false
): MessageReceivedEvent {
    val channel = jdaFake<MessageChannelUnion> { method, _ -> if (method.name == "getType") channelType else null }
    val author = jdaFake<User> { method, _ ->
        when (method.name) {
            "isBot" -> isBot
            "getIdLong" -> MEMBER_ID
            else -> null
        }
    }
    val message = jdaFake<Message> { method, _ ->
        when (method.name) {
            "getIdLong" -> 1L
            "getChannel" -> channel
            "getChannelId" -> "$channelId"
            "getContentRaw" -> text
            "isWebhookMessage" -> isWebhookMessage
            "getAuthor" -> author
            "getMember" -> member
            else -> null
        }
    }
    return MessageReceivedEvent(jda, 0L, message)
}
