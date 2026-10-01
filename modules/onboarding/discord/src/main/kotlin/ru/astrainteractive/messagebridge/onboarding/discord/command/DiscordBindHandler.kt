package ru.astrainteractive.messagebridge.onboarding.discord.command

import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.channel.ChannelType
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import ru.astrainteractive.astralibs.localization.text.LocalizedText
import ru.astrainteractive.klibs.kstorage.api.CachedKrate
import ru.astrainteractive.klibs.kstorage.api.MutableKrate
import ru.astrainteractive.klibs.kstorage.api.getValue
import ru.astrainteractive.klibs.mikro.core.logging.JUtiltLogger
import ru.astrainteractive.klibs.mikro.core.logging.Logger
import ru.astrainteractive.messagebridge.core.api.config.PluginConfiguration
import ru.astrainteractive.messagebridge.core.api.util.toMessengerText
import ru.astrainteractive.messagebridge.messenger.discord.api.api.DiscordMessageSender
import ru.astrainteractive.messagebridge.onboarding.api.config.OnboardingTranslation
import ru.astrainteractive.messagebridge.onboarding.api.impl.BindCodes

internal class DiscordBindHandler(
    private val bindCodes: BindCodes,
    private val configKrate: MutableKrate<Result<PluginConfiguration>>,
    private val messageSender: DiscordMessageSender,
    translationKrate: CachedKrate<OnboardingTranslation>,
) : Logger by JUtiltLogger("MessageBridge-DiscordBindHandler") {
    private val translation by translationKrate

    private suspend fun reply(event: MessageReceivedEvent, text: LocalizedText) {
        messageSender.reply(event.message, text.toMessengerText())
    }

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
        configKrate
            .saveAndGet { result ->
                result.map { config -> config.copy(jdaConfig = config.jdaConfig.copy(channelId = channel.id)) }
            }
            .onFailure { failure ->
                error(failure) { "#bind config.yml cannot be read, so channel ${channel.id} is not saved" }
                return reply(event, bind.configBroken)
            }
        info { "#bind channel #${channel.name} (${channel.id}) on ${channel.guild.name} is bound by ${member.id}" }
        reply(event, bind.boundChannel)
        onBound.invoke(bind.bound(channel = channel.name, guild = channel.guild.name))
    }
}
