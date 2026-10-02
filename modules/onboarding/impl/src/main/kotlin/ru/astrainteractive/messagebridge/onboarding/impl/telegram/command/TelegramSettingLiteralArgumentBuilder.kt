package ru.astrainteractive.messagebridge.onboarding.impl.telegram.command

import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import ru.astrainteractive.astralibs.command.api.brigadier.command.MultiplatformCommand
import ru.astrainteractive.astralibs.localization.component.LocalizableComponent
import ru.astrainteractive.astralibs.server.KAudience
import ru.astrainteractive.klibs.kstorage.api.CachedKrate
import ru.astrainteractive.klibs.kstorage.api.MutableKrate
import ru.astrainteractive.klibs.kstorage.api.getValue
import ru.astrainteractive.messagebridge.core.api.command.CommandExceptionHandler
import ru.astrainteractive.messagebridge.core.api.config.PluginConfiguration
import ru.astrainteractive.messagebridge.core.api.util.describe
import ru.astrainteractive.messagebridge.onboarding.api.config.OnboardingTranslation
import ru.astrainteractive.messagebridge.onboarding.api.permission.OnboardingPermission
import ru.astrainteractive.messagebridge.onboarding.impl.secret.internal.masked
import ru.astrainteractive.messagebridge.onboarding.impl.secret.internal.withoutCredentials
import ru.astrainteractive.messagebridge.onboarding.impl.telegram.internal.TelegramSettings

internal class TelegramSettingLiteralArgumentBuilder(
    private val settings: TelegramSettings,
    private val configKrate: MutableKrate<Result<PluginConfiguration>>,
    private val multiplatformCommand: MultiplatformCommand,
    private val commandExceptionHandler: CommandExceptionHandler,
    translationKrate: CachedKrate<OnboardingTranslation>
) {
    private val translation by translationKrate

    private fun topicSaved(topicId: String): LocalizableComponent {
        if (topicId.isEmpty()) return translation.setup.saved.topicRemoved
        return translation.setup.saved.topic(topicId)
    }

    private fun apiUrlSaved(apiUrl: String): LocalizableComponent {
        if (apiUrl.isEmpty()) return translation.setup.saved.apiUrlRemoved
        return translation.setup.saved.apiUrl(apiUrl.withoutCredentials())
    }

    private fun saveToken(sender: KAudience, token: String) {
        configKrate
            .saveAndGet { loaded ->
                loaded.map { config -> config.copy(tgConfig = config.tgConfig.copy(token = token)) }
            }
            .onSuccess { _ -> sender.sendMessage(translation.setup.saved.token(token.masked())) }
            .onFailure { t -> sender.sendMessage(translation.setup.configBroken(t.describe())) }
    }

    private fun saveChat(sender: KAudience, chatId: String) {
        configKrate
            .saveAndGet { loaded ->
                loaded.map { config -> config.copy(tgConfig = config.tgConfig.copy(chatID = chatId)) }
            }
            .onSuccess { _ -> sender.sendMessage(translation.setup.saved.chat(chatId)) }
            .onFailure { t -> sender.sendMessage(translation.setup.configBroken(t.describe())) }
    }

    private fun saveTopic(sender: KAudience, topicId: String) {
        configKrate
            .saveAndGet { loaded ->
                loaded.map { config -> config.copy(tgConfig = config.tgConfig.copy(topicID = topicId)) }
            }
            .onSuccess { _ -> sender.sendMessage(topicSaved(topicId)) }
            .onFailure { t -> sender.sendMessage(translation.setup.configBroken(t.describe())) }
    }

    private fun saveApiUrl(sender: KAudience, apiUrl: String) {
        configKrate
            .saveAndGet { loaded ->
                loaded.map { config -> config.copy(tgConfig = config.tgConfig.copy(apiUrl = apiUrl)) }
            }
            .onSuccess { _ -> sender.sendMessage(apiUrlSaved(apiUrl)) }
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

    private fun chatCommand() = with(multiplatformCommand) {
        command("chat") {
            argument("chat_id", StringArgumentType.word()) { chatArg ->
                runs(commandExceptionHandler::handle) { ctx ->
                    ctx.requirePermission(OnboardingPermission.Setup)
                    settings.chat(ctx.requireArgument(chatArg))
                        .onSuccess { chatId -> saveChat(ctx.getSender(), chatId) }
                        .onFailure { t -> commandExceptionHandler.handle(ctx, t) }
                }
            }
        }
    }

    private fun topicCommand() = with(multiplatformCommand) {
        command("topic") {
            argument("topic_id", StringArgumentType.word()) { topicArg ->
                hints { _ -> listOf(TelegramSettings.NONE) }
                runs(commandExceptionHandler::handle) { ctx ->
                    ctx.requirePermission(OnboardingPermission.Setup)
                    settings.topic(ctx.requireArgument(topicArg))
                        .onSuccess { topicId -> saveTopic(ctx.getSender(), topicId) }
                        .onFailure { t -> commandExceptionHandler.handle(ctx, t) }
                }
            }
        }
    }

    private fun apiUrlCommand() = with(multiplatformCommand) {
        command("api-url") {
            argument("url", StringArgumentType.greedyString()) { urlArg ->
                hints { _ -> listOf(TelegramSettings.DEFAULT) }
                runs(commandExceptionHandler::handle) { ctx ->
                    ctx.requirePermission(OnboardingPermission.Setup)
                    settings.apiUrl(ctx.requireArgument(urlArg))
                        .onSuccess { apiUrl -> saveApiUrl(ctx.getSender(), apiUrl) }
                        .onFailure { t -> commandExceptionHandler.handle(ctx, t) }
                }
            }
        }
    }

    fun create(): List<LiteralArgumentBuilder<Any>> {
        return listOf(tokenCommand(), chatCommand(), topicCommand(), apiUrlCommand())
    }
}
