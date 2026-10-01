package ru.astrainteractive.messagebridge.messenger.discord.messaging

import kotlinx.coroutines.flow.StateFlow
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import ru.astrainteractive.astralibs.localization.component.LocalizableComponent
import ru.astrainteractive.klibs.kstorage.api.CachedKrate
import ru.astrainteractive.klibs.kstorage.api.getValue
import ru.astrainteractive.messagebridge.core.PluginConfiguration
import ru.astrainteractive.messagebridge.core.PluginTranslation
import ru.astrainteractive.messagebridge.core.mapping.toMessengerText
import ru.astrainteractive.messagebridge.messaging.setup.DiagnosticCheck
import ru.astrainteractive.messagebridge.messenger.discord.mapping.DiscordFailureMapper
import ru.astrainteractive.messagebridge.messenger.discord.model.DiscordConnection
import ru.astrainteractive.messagebridge.messenger.discord.model.DiscordPermissions
import ru.astrainteractive.messagebridge.messenger.discord.model.awaitSettled
import ru.astrainteractive.messagebridge.messenger.discord.util.RestActionExt.awaitCatching
import ru.astrainteractive.messagebridge.messenger.discord.util.findTextChannel
import kotlin.time.Duration.Companion.seconds

/** Checks the settings and the rights of the bot in the order an admin sets them up, and stops at the first gap. */
internal class DiscordDiagnostics(
    private val connection: StateFlow<DiscordConnection>,
    private val failureMapper: DiscordFailureMapper,
    configKrate: CachedKrate<PluginConfiguration>,
    translationKrate: CachedKrate<PluginTranslation>,
) {
    private val config by configKrate
    private val translation by translationKrate

    suspend fun diagnose(): List<DiagnosticCheck> = mutableListOf<DiagnosticCheck>().apply { addChecks() }

    private suspend fun MutableList<DiagnosticCheck>.addChecks() {
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
        val channel = findChannel(jda, jdaConfig.channelId) ?: return
        addPermissionCheck(channel)
        config.link?.let { link -> addLinkRoleCheck(channel.guild, link) }
        addTestMessageCheck(channel)
    }

    private fun MutableList<DiagnosticCheck>.findChannel(jda: JDA, channelId: String): TextChannel? {
        val discord = translation.discord
        val channel = jda.findTextChannel(channelId)
        when {
            jda.guilds.isEmpty() -> addError(discord.check.noGuilds)
            channelId.isBlank() -> addError(discord.errors.channelNotSet)
            channel == null -> addError(discord.errors.channelNotFound(channelId))
        }
        return channel
    }

    private fun MutableList<DiagnosticCheck>.addPermissionCheck(channel: TextChannel) {
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

    private fun MutableList<DiagnosticCheck>.addLinkRoleCheck(guild: Guild, link: PluginConfiguration.Link) {
        val check = translation.discord.check
        val role = link.linkDiscordRole.toLongOrNull()?.let(guild::getRoleById)
        when {
            role == null -> addWarning(check.linkRoleNotFound(role = link.linkDiscordRole, guild = guild.name))
            !guild.selfMember.hasPermission(DiscordPermissions.LINK) -> addWarning(check.linkRoleNoPermission)
            !guild.selfMember.canInteract(role) -> addWarning(check.linkRoleAboveBot(role.name))
            else -> addOk(check.linkRoleOk(role.name))
        }
    }

    private suspend fun MutableList<DiagnosticCheck>.addTestMessageCheck(channel: TextChannel) {
        val check = translation.discord.check
        channel.sendMessage(check.testMessage.toMessengerText())
            .awaitCatching()
            .onSuccess { addOk(check.testMessageSent(channel.name)) }
            .onFailure { throwable ->
                val reason = failureMapper.toText(failureMapper.map(throwable, config.jdaConfig), translation.discord)
                addError(check.testMessageFailed(reason))
            }
    }

    private fun MutableList<DiagnosticCheck>.addOk(message: LocalizableComponent) {
        add(DiagnosticCheck(DiagnosticCheck.Level.OK, message))
    }

    private fun MutableList<DiagnosticCheck>.addWarning(message: LocalizableComponent) {
        add(DiagnosticCheck(DiagnosticCheck.Level.WARNING, message))
    }

    private fun MutableList<DiagnosticCheck>.addError(message: LocalizableComponent) {
        add(DiagnosticCheck(DiagnosticCheck.Level.ERROR, message))
    }

    private companion object {
        val CONNECTION_WAIT = 10.seconds
    }
}
