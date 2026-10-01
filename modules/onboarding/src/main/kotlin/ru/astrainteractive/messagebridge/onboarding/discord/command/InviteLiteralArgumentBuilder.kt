package ru.astrainteractive.messagebridge.onboarding.discord.command

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.format.TextDecoration
import ru.astrainteractive.astralibs.command.api.brigadier.command.MultiplatformCommand
import ru.astrainteractive.astralibs.localization.component.asLocalizableComponent
import ru.astrainteractive.astralibs.server.KAudience
import ru.astrainteractive.klibs.kstorage.api.CachedKrate
import ru.astrainteractive.klibs.kstorage.api.getValue
import ru.astrainteractive.messagebridge.core.api.command.CommandExceptionHandler
import ru.astrainteractive.messagebridge.core.api.config.PluginTranslation
import ru.astrainteractive.messagebridge.core.api.permission.PluginPermission
import ru.astrainteractive.messagebridge.onboarding.discord.internal.DiscordMessenger
import ru.astrainteractive.messagebridge.onboarding.status.internal.StatusText

internal class InviteLiteralArgumentBuilder(
    private val messenger: DiscordMessenger,
    private val statusText: StatusText,
    private val ioScope: CoroutineScope,
    private val multiplatformCommand: MultiplatformCommand,
    private val commandExceptionHandler: CommandExceptionHandler,
    translationKrate: CachedKrate<PluginTranslation>
) {
    private val translation by translationKrate

    private suspend fun invite(sender: KAudience) {
        val url = messenger.onboarding.inviteUrl()
        if (url == null) {
            sender.sendMessage(translation.setup.inviteUnavailable)
            sender.sendMessage(statusText.currentStateOf(messenger))
            return
        }
        val link = Component.text(url)
            .decorate(TextDecoration.UNDERLINED)
            .clickEvent(ClickEvent.openUrl(url))
            .asLocalizableComponent()
        sender.sendMessage(translation.setup.inviteLink(link))
    }

    fun create(): LiteralArgumentBuilder<Any> {
        return with(multiplatformCommand) {
            command("invite") {
                runs(commandExceptionHandler::handle) { ctx ->
                    ctx.requirePermission(PluginPermission.Setup)
                    val sender = ctx.getSender()
                    ioScope.launch(commandExceptionHandler.coroutineExceptionHandler(ctx)) { invite(sender) }
                }
            }
        }
    }
}
