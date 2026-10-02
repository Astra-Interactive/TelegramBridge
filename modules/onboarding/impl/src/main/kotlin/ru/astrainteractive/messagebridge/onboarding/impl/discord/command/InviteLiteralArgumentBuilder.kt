package ru.astrainteractive.messagebridge.onboarding.impl.discord.command

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.format.TextDecoration
import ru.astrainteractive.astralibs.command.api.brigadier.command.MultiplatformCommand
import ru.astrainteractive.astralibs.localization.component.asLocalizableComponent
import ru.astrainteractive.astralibs.server.KAudience
import ru.astrainteractive.klibs.kstorage.api.CachedKrate
import ru.astrainteractive.klibs.kstorage.api.getValue
import ru.astrainteractive.messagebridge.core.api.command.CommandExceptionHandler
import ru.astrainteractive.messagebridge.onboarding.api.config.OnboardingTranslation
import ru.astrainteractive.messagebridge.onboarding.api.permission.OnboardingPermission
import ru.astrainteractive.messagebridge.onboarding.impl.discord.internal.DiscordMessenger

internal class InviteLiteralArgumentBuilder(
    private val messenger: DiscordMessenger,
    private val multiplatformCommand: MultiplatformCommand,
    private val commandExceptionHandler: CommandExceptionHandler,
    translationKrate: CachedKrate<OnboardingTranslation>
) {
    private val translation by translationKrate

    private fun invite(sender: KAudience) {
        val url = messenger.onboarding.inviteUrl()
        if (url == null) {
            sender.sendMessage(translation.setup.inviteUnavailable)
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
                    ctx.requirePermission(OnboardingPermission.Setup)
                    invite(ctx.getSender())
                }
            }
        }
    }
}
