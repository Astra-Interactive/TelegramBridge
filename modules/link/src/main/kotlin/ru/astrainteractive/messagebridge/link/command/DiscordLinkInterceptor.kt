package ru.astrainteractive.messagebridge.link.command

import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.channel.ChannelType
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.exceptions.ErrorResponseException
import net.dv8tion.jda.api.requests.ErrorResponse
import ru.astrainteractive.klibs.kstorage.api.CachedKrate
import ru.astrainteractive.klibs.kstorage.api.getValue
import ru.astrainteractive.klibs.mikro.core.coroutines.propagateCancellationException
import ru.astrainteractive.klibs.mikro.core.logging.JUtiltLogger
import ru.astrainteractive.klibs.mikro.core.logging.Logger
import ru.astrainteractive.klibs.mikro.core.util.tryCast
import ru.astrainteractive.messagebridge.core.config.PluginConfiguration
import ru.astrainteractive.messagebridge.core.config.PluginTranslation
import ru.astrainteractive.messagebridge.core.mapping.toMessengerText
import ru.astrainteractive.messagebridge.link.dao.model.MessengerAccount
import ru.astrainteractive.messagebridge.link.internal.DiscordRoleController
import ru.astrainteractive.messagebridge.link.mapping.asMessage
import ru.astrainteractive.messagebridge.link.model.LinkResponse
import ru.astrainteractive.messagebridge.link.usecase.LinkAccountUseCase
import ru.astrainteractive.messagebridge.messaging.api.MessageInterceptor
import ru.astrainteractive.messagebridge.messaging.model.Interception
import ru.astrainteractive.messagebridge.messenger.discord.util.await

private class BridgeChannelNotFoundError(channelId: String) :
    Exception("The bot cannot see the Discord bridge channel $channelId")

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

    private suspend fun bridgeMemberOf(event: MessageReceivedEvent): Result<Member?> {
        event.member?.let { member -> return Result.success(member) }
        val channelId = config.jdaConfig.channelId
        val guild = event.jda
            .getTextChannelById(channelId)
            ?.guild
            ?: return Result.failure(BridgeChannelNotFoundError(channelId))
        return runCatching { guild.retrieveMemberById(event.author.idLong).await() }
            .propagateCancellationException()
            .fold(
                onSuccess = { member -> Result.success(member) },
                onFailure = { t ->
                    if (t.tryCast<ErrorResponseException>()?.errorResponse == ErrorResponse.UNKNOWN_MEMBER) {
                        info { "#bridgeMemberOf ${event.author.idLong} is not on the server: ${t.message}" }
                        Result.success(null)
                    } else {
                        Result.failure(t)
                    }
                }
            )
    }

    override suspend fun intercept(event: MessageReceivedEvent): Interception {
        val code = linkCode(event) ?: return Interception.Pass
        val member = bridgeMemberOf(event).getOrElse { t ->
            error { "#intercept could not check that ${event.author.idLong} is on the server: ${t.message}" }
            return Interception.Reply(translation.link.unknownError.toMessengerText())
        }
        if (member == null) return Interception.Reply(translation.link.notServerMember.toMessengerText())
        val account = MessengerAccount.Discord(
            id = event.author.idLong,
            name = member.effectiveName
        )
        val response = linkAccountUseCase.link(code, account)
        if (response == LinkResponse.Linked) {
            discordRoleController.addLinkedRole(account.id)
        }
        return Interception.Reply(response.asMessage(translation.link).toMessengerText())
    }
}
