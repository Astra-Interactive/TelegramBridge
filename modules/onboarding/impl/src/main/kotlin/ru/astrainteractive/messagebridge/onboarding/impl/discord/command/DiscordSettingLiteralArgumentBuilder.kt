package ru.astrainteractive.messagebridge.onboarding.impl.discord.command

import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import ru.astrainteractive.astralibs.command.api.brigadier.command.MultiplatformCommand
import ru.astrainteractive.astralibs.server.KAudience
import ru.astrainteractive.klibs.kstorage.api.CachedKrate
import ru.astrainteractive.klibs.kstorage.api.MutableKrate
import ru.astrainteractive.klibs.kstorage.api.getValue
import ru.astrainteractive.messagebridge.core.api.command.CommandExceptionHandler
import ru.astrainteractive.messagebridge.core.api.config.PluginConfiguration
import ru.astrainteractive.messagebridge.core.api.util.describe
import ru.astrainteractive.messagebridge.onboarding.api.config.OnboardingTranslation
import ru.astrainteractive.messagebridge.onboarding.api.permission.OnboardingPermission
import ru.astrainteractive.messagebridge.onboarding.impl.discord.internal.DiscordSettings
import ru.astrainteractive.messagebridge.onboarding.impl.secret.internal.masked

internal class DiscordSettingLiteralArgumentBuilder(
    private val settings: DiscordSettings,
    private val configKrate: MutableKrate<Result<PluginConfiguration>>,
    private val multiplatformCommand: MultiplatformCommand,
    private val commandExceptionHandler: CommandExceptionHandler,
    translationKrate: CachedKrate<OnboardingTranslation>
) {
    private val translation by translationKrate

    private fun saveToken(sender: KAudience, token: String) {
        configKrate
            .saveAndGet { loaded ->
                loaded.map { config -> config.copy(jdaConfig = config.jdaConfig.copy(token = token)) }
            }
            .onSuccess { _ -> sender.sendMessage(translation.setup.saved.token(token.masked())) }
            .onFailure { t -> sender.sendMessage(translation.setup.configBroken(t.describe())) }
    }

    private fun saveChannel(sender: KAudience, channelId: String) {
        configKrate
            .saveAndGet { loaded ->
                loaded.map { config -> config.copy(jdaConfig = config.jdaConfig.copy(channelId = channelId)) }
            }
            .onSuccess { _ -> sender.sendMessage(translation.setup.saved.channel(channelId)) }
            .onFailure { t -> sender.sendMessage(translation.setup.configBroken(t.describe())) }
    }

    private fun saveActivity(sender: KAudience, activity: String) {
        configKrate
            .saveAndGet { loaded ->
                loaded.map { config -> config.copy(jdaConfig = config.jdaConfig.copy(activity = activity)) }
            }
            .onSuccess { _ -> sender.sendMessage(translation.setup.saved.activity(activity)) }
            .onFailure { t -> sender.sendMessage(translation.setup.configBroken(t.describe())) }
    }

    private fun tokenCommand() = with(multiplatformCommand) {
        command("token") {
            argument("token", StringArgumentType.greedyString()) { tokenArg ->
                runs(commandExceptionHandler::handle) { ctx ->
                    ctx.requirePermission(OnboardingPermission.Setup)
                    val sender = ctx.getSender()
                    settings.token(sender, ctx.requireArgument(tokenArg))
                        .onSuccess { token -> saveToken(sender, token) }
                        .onFailure { t -> commandExceptionHandler.handle(ctx, t) }
                }
            }
        }
    }

    private fun channelCommand() = with(multiplatformCommand) {
        command("channel") {
            argument("channel_id", StringArgumentType.word()) { channelArg ->
                runs(commandExceptionHandler::handle) { ctx ->
                    ctx.requirePermission(OnboardingPermission.Setup)
                    settings.channel(ctx.requireArgument(channelArg))
                        .onSuccess { channelId -> saveChannel(ctx.getSender(), channelId) }
                        .onFailure { t -> commandExceptionHandler.handle(ctx, t) }
                }
            }
        }
    }

    private fun activityCommand() = with(multiplatformCommand) {
        command("activity") {
            argument("text", StringArgumentType.greedyString()) { textArg ->
                runs(commandExceptionHandler::handle) { ctx ->
                    ctx.requirePermission(OnboardingPermission.Setup)
                    saveActivity(ctx.getSender(), ctx.requireArgument(textArg).trim())
                }
            }
        }
    }

    fun create(): List<LiteralArgumentBuilder<Any>> {
        return listOf(tokenCommand(), channelCommand(), activityCommand())
    }
}
