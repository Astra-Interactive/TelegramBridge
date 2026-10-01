package ru.astrainteractive.messagebridge.messenger.discord.event

import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.channel.ChannelType
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import ru.astrainteractive.astralibs.localization.text.LocalizedText
import ru.astrainteractive.klibs.kstorage.api.CachedKrate
import ru.astrainteractive.klibs.kstorage.api.MutableKrate
import ru.astrainteractive.klibs.kstorage.api.getValue
import ru.astrainteractive.klibs.mikro.core.logging.JUtiltLogger
import ru.astrainteractive.klibs.mikro.core.logging.Logger
import ru.astrainteractive.messagebridge.core.PluginConfiguration
import ru.astrainteractive.messagebridge.core.PluginTranslation
import ru.astrainteractive.messagebridge.core.mapping.toMessengerText
import ru.astrainteractive.messagebridge.messenger.discord.messaging.DiscordMessageSender
import ru.astrainteractive.messagebridge.onboarding.bind.BindCodes

/**
 * Binds the channel a `!bind <code>` is sent to. The author must be able to manage that channel, so a code seen
 * by someone else cannot move the bridge into their server.
 */
internal class DiscordBindHandler(
    private val bindCodes: BindCodes,
    private val configKrate: MutableKrate<Result<PluginConfiguration>>,
    private val messageSender: DiscordMessageSender,
    translationKrate: CachedKrate<PluginTranslation>,
) : Logger by JUtiltLogger("MessageBridge-DiscordBindHandler") {
    private val translation by translationKrate

    suspend fun bind(code: String, event: MessageReceivedEvent) {
        val bind = translation.discord.bind
        val member = event.member ?: return
        val refusal = when {
            !bindCodes.isValid(code) -> bind.codeInvalid
            !event.isFromType(ChannelType.TEXT) -> bind.wrongChannelType
            !member.hasPermission(event.guildChannel, Permission.MANAGE_CHANNEL) -> bind.noPermission
            else -> null
        }
        if (refusal != null) return reply(event, refusal)
        val onBound = bindCodes.consume(code) ?: return reply(event, bind.codeInvalid)
        val channel = event.channel.asTextChannel()
        configKrate.save { result ->
            result.map { config -> config.copy(jdaConfig = config.jdaConfig.copy(channelId = channel.id)) }
        }
        info { "#bind channel #${channel.name} (${channel.id}) on ${channel.guild.name} is bound by ${member.id}" }
        reply(event, bind.boundChannel)
        onBound.invoke(bind.bound(channel = channel.name, guild = channel.guild.name))
    }

    private suspend fun reply(event: MessageReceivedEvent, text: LocalizedText) {
        messageSender.reply(event.message, text.toMessengerText())
    }
}
