package ru.astrainteractive.messagebridge.onboarding

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import ru.astrainteractive.astralibs.command.api.brigadier.command.MultiplatformCommand
import ru.astrainteractive.klibs.kstorage.api.CachedKrate
import ru.astrainteractive.klibs.kstorage.api.getValue
import ru.astrainteractive.messagebridge.core.PluginTranslation
import ru.astrainteractive.messagebridge.core.command.CommandExceptionHandler

/** `/mb` lists its subcommands to anyone; each subcommand checks its own permission. */
internal class MbLiteralArgumentBuilder(
    private val subcommands: List<LiteralArgumentBuilder<Any>>,
    private val multiplatformCommand: MultiplatformCommand,
    private val commandExceptionHandler: CommandExceptionHandler,
    translationKrate: CachedKrate<PluginTranslation>
) {
    private val translation by translationKrate

    fun create(): LiteralArgumentBuilder<Any> {
        return with(multiplatformCommand) {
            command("mb") {
                runs(commandExceptionHandler::handle) { ctx -> ctx.getSender().sendMessage(translation.setup.help) }
                literal("help") {
                    runs(commandExceptionHandler::handle) { ctx -> ctx.getSender().sendMessage(translation.setup.help) }
                }
                subcommands.forEach { subcommand -> then(subcommand) }
            }
        }
    }
}
