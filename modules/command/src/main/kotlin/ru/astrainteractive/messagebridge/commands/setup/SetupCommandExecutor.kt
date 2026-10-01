package ru.astrainteractive.messagebridge.commands.setup

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.format.TextDecoration
import ru.astrainteractive.astralibs.localization.component.LocalizableComponent
import ru.astrainteractive.astralibs.localization.component.asLocalizableComponent
import ru.astrainteractive.astralibs.server.KAudience
import ru.astrainteractive.klibs.kstorage.api.CachedKrate
import ru.astrainteractive.klibs.kstorage.api.StateFlowMutableKrate
import ru.astrainteractive.klibs.kstorage.api.getValue
import ru.astrainteractive.messagebridge.core.PluginConfiguration
import ru.astrainteractive.messagebridge.core.PluginTranslation
import ru.astrainteractive.messagebridge.core.config.YamlConfigFile
import ru.astrainteractive.messagebridge.messaging.setup.DiscordSetup
import ru.astrainteractive.messagebridge.messaging.setup.MessengerStatus
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/** Saves what the /mb commands set into config.yml and tells the sender how the bot took it. */
internal class SetupCommandExecutor(
    private val configFile: YamlConfigFile<PluginConfiguration>,
    private val configKrate: StateFlowMutableKrate<PluginConfiguration>,
    translationKrate: CachedKrate<PluginTranslation>,
    private val connectionTimeout: Duration = CONNECTION_TIMEOUT
) {
    private val translation by translationKrate
    private val formatter = SetupStatusFormatter(translationKrate)

    /**
     * Saving reads config.yml again and falls back to the last settings that worked when the file has an error,
     * so saving over such a file would replace everything the admin wrote in it.
     *
     * @return whether config.yml can be saved; if not, [sender] is told why
     */
    private fun isConfigWritable(sender: KAudience): Boolean {
        val error = configFile.check()
        if (error != null) sender.sendMessage(translation.setup.configBroken(error))
        return error == null
    }

    fun <C> save(
        sender: KAudience,
        messenger: SetupMessenger<C>,
        saved: LocalizableComponent,
        edit: (C) -> C
    ) {
        if (!isConfigWritable(sender)) return
        configKrate.save { config -> messenger.edit(config, edit) }
        sender.sendMessage(saved)
    }

    /**
     * Saves a setting the bot connects with, then waits for the bot to connect or fail. The status is observed
     * before saving, so a bot that fails at once is not missed.
     */
    suspend fun <C> saveAndConnect(
        sender: KAudience,
        messenger: SetupMessenger<C>,
        saved: LocalizableComponent,
        edit: (C) -> C
    ) {
        if (!isConfigWritable(sender)) return
        val updated = messenger.edit(configKrate.cachedValue, edit)
        val isReconnecting = messenger.hasToken(updated) &&
            messenger.sectionOf(updated) != messenger.sectionOf(configKrate.cachedValue)
        if (!isReconnecting) {
            configKrate.save { config -> messenger.edit(config, edit) }
            sender.sendMessage(saved)
            if (messenger.hasToken(updated)) {
                sender.sendMessage(formatter.connectionResultOf(messenger, messenger.setup.status.value))
            }
            return
        }
        val status = coroutineScope {
            val nextStatus = async(start = CoroutineStart.UNDISPATCHED) {
                messenger.setup.status
                    .drop(1)
                    .first { status -> status != MessengerStatus.Connecting }
            }
            configKrate.save { config -> messenger.edit(config, edit) }
            sender.sendMessage(saved)
            val result = withTimeoutOrNull(connectionTimeout) { nextStatus.await() }
            nextStatus.cancel()
            result ?: messenger.setup.status.value
        }
        sender.sendMessage(formatter.connectionResultOf(messenger, status))
    }

    fun issueBindCode(
        sender: KAudience,
        messenger: SetupMessenger<*>,
        instruction: (code: String) -> LocalizableComponent
    ) {
        val status = messenger.setup.status.value
        if (status == MessengerStatus.Disabled) {
            sender.sendMessage(formatter.stateOf(messenger, status))
            return
        }
        val code = messenger.setup.issueBindCode { text -> sender.sendMessage(text) }
        sender.sendMessage(instruction.invoke(code))
    }

    suspend fun check(sender: KAudience, messenger: SetupMessenger<*>) {
        sender.sendMessage(translation.setup.checkStarted(messenger.name))
        messenger.setup.diagnose().forEach { check -> sender.sendMessage(formatter.checkLineOf(check)) }
    }

    suspend fun invite(sender: KAudience, discord: SetupMessenger<*>, setup: DiscordSetup) {
        val url = setup.inviteUrl()
        if (url == null) {
            sender.sendMessage(translation.setup.inviteUnavailable)
            sender.sendMessage(formatter.stateOf(discord, setup.status.value))
            return
        }
        val link = Component.text(url)
            .decorate(TextDecoration.UNDERLINED)
            .clickEvent(ClickEvent.openUrl(url))
            .asLocalizableComponent()
        sender.sendMessage(translation.setup.inviteLink(link))
    }

    fun telegramStatus(messenger: SetupMessenger<PluginConfiguration.TelegramConfig>): List<LocalizableComponent> {
        return formatter.telegram(messenger, configKrate.cachedValue)
    }

    fun discordStatus(messenger: SetupMessenger<PluginConfiguration.JdaConfig>): List<LocalizableComponent> {
        return formatter.discord(messenger, configKrate.cachedValue)
    }

    companion object {
        val CONNECTION_TIMEOUT = 20.seconds
    }
}
