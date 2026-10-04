package ru.astrainteractive.messagebridge.link.command

import kotlinx.coroutines.future.await
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.channel.ChannelType
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import ru.astrainteractive.klibs.kstorage.api.CachedKrate
import ru.astrainteractive.klibs.kstorage.api.getValue
import ru.astrainteractive.klibs.mikro.core.coroutines.propagateCancellationException
import ru.astrainteractive.klibs.mikro.core.logging.JUtiltLogger
import ru.astrainteractive.klibs.mikro.core.logging.Logger
import ru.astrainteractive.messagebridge.core.PluginConfiguration
import ru.astrainteractive.messagebridge.core.PluginTranslation
import ru.astrainteractive.messagebridge.core.mapping.toMessengerText
import ru.astrainteractive.messagebridge.link.internal.DiscordRoleController
import ru.astrainteractive.messagebridge.link.mapping.asMessage
import ru.astrainteractive.messagebridge.link.model.LinkResponse
import ru.astrainteractive.messagebridge.link.player.model.MessengerAccount
import ru.astrainteractive.messagebridge.link.usecase.LinkAccountUseCase
import ru.astrainteractive.messagebridge.messaging.api.MessageInterceptor
import ru.astrainteractive.messagebridge.messaging.model.Interception

internal class DiscordLinkInterceptor(
    private val linkAccountUseCase: LinkAccountUseCase,
    private val discordRoleController: DiscordRoleController,
    configKrate: CachedKrate<PluginConfiguration>,
    translationKrate: CachedKrate<PluginTranslation>
) : MessageInterceptor<MessageReceivedEvent>, Logger by JUtiltLogger("MessageBridge-DiscordLinkInterceptor") {
    private val config by configKrate
    private val translation by translationKrate

    private fun linkCode(event: MessageReceivedEvent): Int? {
        val contentRaw = event.message.contentRaw
        if (event.channelType == ChannelType.PRIVATE) {
            return contentRaw.toIntOrNull() ?: INVALID_LINK_CODE
        }
        return contentRaw.toLinkCode()
    }

    private suspend fun bridgeMemberOf(event: MessageReceivedEvent): Member? {
        event.member?.let { member -> return member }
        val guild = event.jda
            .getTextChannelById(config.jdaConfig.channelId)
            ?.guild
            ?: return null
        return runCatching { guild.retrieveMemberById(event.author.idLong).submit().await() }
            .propagateCancellationException()
            .onFailure { t -> info { "#bridgeMemberOf ${event.author.idLong} is not on the server: ${t.message}" } }
            .getOrNull()
    }

    override suspend fun intercept(event: MessageReceivedEvent): Interception {
        val code = linkCode(event) ?: return Interception.Pass
        val member = bridgeMemberOf(event)
        val account = MessengerAccount.Discord(
            id = event.author.idLong,
            name = member?.effectiveName ?: event.author.effectiveName
        )
        val response = linkAccountUseCase.link(code, account)
        if (response == LinkResponse.Linked && member != null) {
            discordRoleController.addLinkedRole(account.id)
        }
        return Interception.Reply(response.asMessage(translation.link).toMessengerText())
    }
}
