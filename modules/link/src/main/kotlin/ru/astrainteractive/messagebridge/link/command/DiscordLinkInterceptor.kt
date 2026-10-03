package ru.astrainteractive.messagebridge.link.command

import net.dv8tion.jda.api.entities.channel.ChannelType
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import ru.astrainteractive.klibs.kstorage.api.CachedKrate
import ru.astrainteractive.klibs.kstorage.api.getValue
import ru.astrainteractive.messagebridge.core.PluginTranslation
import ru.astrainteractive.messagebridge.core.mapping.toMessengerText
import ru.astrainteractive.messagebridge.link.api.LinkApi
import ru.astrainteractive.messagebridge.link.mapping.asMessage
import ru.astrainteractive.messagebridge.messaging.api.MessageInterceptor
import ru.astrainteractive.messagebridge.messaging.model.Interception

internal class DiscordLinkInterceptor(
    private val linkApi: LinkApi,
    translationKrate: CachedKrate<PluginTranslation>
) : MessageInterceptor<MessageReceivedEvent> {
    private val translation by translationKrate

    private fun linkCode(event: MessageReceivedEvent): Int? {
        val contentRaw = event.message.contentRaw
        if (event.channelType == ChannelType.PRIVATE) {
            return contentRaw.toIntOrNull() ?: INVALID_LINK_CODE
        }
        return contentRaw.toLinkCode()
    }

    override suspend fun intercept(event: MessageReceivedEvent): Interception {
        val code = linkCode(event) ?: return Interception.Pass
        val member = event.member ?: return Interception.Consumed
        val response = linkApi.linkDiscord(code, member)
        return Interception.Reply(response.asMessage(translation.link).toMessengerText())
    }
}
