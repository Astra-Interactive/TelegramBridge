package ru.astrainteractive.messagebridge.messenger.discord.onboarding.internal

import kotlinx.coroutines.flow.StateFlow
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import ru.astrainteractive.astralibs.localization.component.LocalizableComponent
import ru.astrainteractive.klibs.kstorage.api.CachedKrate
import ru.astrainteractive.klibs.kstorage.api.getValue
import ru.astrainteractive.messagebridge.core.api.config.PluginConfiguration
import ru.astrainteractive.messagebridge.core.api.config.PluginTranslation
import ru.astrainteractive.messagebridge.core.api.mapping.toMessengerText
import ru.astrainteractive.messagebridge.messenger.discord.connection.internal.DiscordIntents
import ru.astrainteractive.messagebridge.messenger.discord.connection.model.DiscordConnection
import ru.astrainteractive.messagebridge.messenger.discord.connection.model.awaitSettled
import ru.astrainteractive.messagebridge.messenger.discord.failure.mapping.DiscordFailureMapper
import ru.astrainteractive.messagebridge.messenger.discord.util.awaitRequest
import ru.astrainteractive.messagebridge.messenger.discord.util.findTextChannel
import ru.astrainteractive.messagebridge.onboarding.api.model.Check
import ru.astrainteractive.messagebridge.onboarding.api.model.CheckLevel
import kotlin.time.Duration.Companion.seconds

internal class DiscordDiagnostics(
    private val connection: StateFlow<DiscordConnection>,
    private val failureMapper: DiscordFailureMapper,
    private val configFlow: StateFlow<PluginConfiguration>,
    translationKrate: CachedKrate<PluginTranslation>,
) {
    private val config: PluginConfiguration
        get() = configFlow.value
    private val translation by translationKrate

    private fun MutableList<Check>.addOk(message: LocalizableComponent) {
        add(Check(CheckLevel.OK, message))
    }

    private fun MutableList<Check>.addWarning(message: LocalizableComponent) {
        add(Check(CheckLevel.WARNING, message))
    }

    private fun MutableList<Check>.addError(message: LocalizableComponent) {
        add(Check(CheckLevel.ERROR, message))
    }

    private fun MutableList<Check>.findChannel(jda: JDA, channelId: String): TextChannel? {
        val discord = translation.discord
        val channel = jda.findTextChannel(channelId)
        when {
            jda.guilds.isEmpty() -> addError(discord.check.noGuilds)
            channelId.isBlank() -> addError(discord.errors.channelNotSet)
            channel == null -> addError(discord.errors.channelNotFound(channelId))
        }
        return channel
    }

    private fun MutableList<Check>.addPermissionCheck(channel: TextChannel) {
        val check = translation.discord.check
        val selfMember = channel.guild.selfMember
        val missing = DiscordPermissions.CHANNEL.filterNot { permission ->
            selfMember.hasPermission(channel, permission)
        }
        if (missing.isEmpty()) {
            addOk(check.permissionsOk(channel = channel.name, guild = channel.guild.name))
        } else {
            val permissions = missing.joinToString(separator = ", ") { permission -> permission.getName() }
            addError(check.missingPermissions(channel = channel.name, permissions = permissions))
        }
    }

    private fun MutableList<Check>.addLinkRoleCheck(guild: Guild, link: PluginConfiguration.Link) {
        val check = translation.discord.check
        val role = link.linkDiscordRole.toLongOrNull()?.let(guild::getRoleById)
        when {
            role == null -> addWarning(check.linkRoleNotFound(role = link.linkDiscordRole, guild = guild.name))
            !guild.selfMember.hasPermission(DiscordPermissions.LINK) -> addWarning(check.linkRoleNoPermission)
            !guild.selfMember.canInteract(role) -> addWarning(check.linkRoleAboveBot(role.name))
            else -> addOk(check.linkRoleOk(role.name))
        }
    }

    private suspend fun MutableList<Check>.addTestMessageCheck(channel: TextChannel) {
        val check = translation.discord.check
        awaitRequest { channel.sendMessage(check.testMessage.toMessengerText()) }
            .onSuccess { _ -> addOk(check.testMessageSent(channel.name)) }
            .onFailure { failure ->
                val reason = failureMapper.toText(failureMapper.map(failure, config.jdaConfig), translation.discord)
                addError(check.testMessageFailed(reason))
            }
    }

    private suspend fun MutableList<Check>.addChecks() {
        val check = translation.discord.check
        val jdaConfig = config.jdaConfig
        if (jdaConfig.token.isBlank()) return addError(check.tokenMissing)
        val jda = when (val state = connection.awaitSettled(CONNECTION_WAIT)) {
            is DiscordConnection.Connected -> state.jda
            is DiscordConnection.Failed -> return addError(failureMapper.toText(state.failure, translation.discord))
            DiscordConnection.Connecting -> return addError(check.connecting)
            DiscordConnection.Disabled -> return addError(check.tokenMissing)
        }
        addOk(check.botWorks(jda.selfUser.name))
        addOk(check.intentsOk(DiscordIntents.privilegedNames(jda.gatewayIntents).joinToString(separator = ", ")))
        val channel = findChannel(jda, jdaConfig.channelId) ?: return
        addPermissionCheck(channel)
        config.link?.let { link -> addLinkRoleCheck(channel.guild, link) }
        addTestMessageCheck(channel)
    }

    suspend fun diagnose(): List<Check> = mutableListOf<Check>().apply { addChecks() }

    private companion object {
        val CONNECTION_WAIT = 10.seconds
    }
}
