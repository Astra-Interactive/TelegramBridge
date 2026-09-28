package ru.astrainteractive.messagebridge.commands.link

import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import ru.astrainteractive.astralibs.command.api.argumenttype.KPlayerArgumentConverter
import ru.astrainteractive.astralibs.command.api.brigadier.command.MultiplatformCommand
import ru.astrainteractive.astralibs.server.bridge.PlatformServer
import ru.astrainteractive.astralibs.server.player.OnlineKPlayer
import ru.astrainteractive.messagebridge.core.command.CommandExceptionHandler

internal class LinkLiteralArgumentBuilder(
    private val executor: LinkCommandExecutor,
    private val ioScope: CoroutineScope,
    private val multiplatformCommand: MultiplatformCommand,
    private val platformServer: PlatformServer,
    private val commandExceptionHandler: CommandExceptionHandler
) {

    private fun launchIntent(intent: LinkCommandExecutor.Intent) {
        ioScope.launch { executor.onIntent(intent) }
    }

    fun create(): LiteralArgumentBuilder<Any> {
        return with(multiplatformCommand) {
            command("link") {
                argument("user", StringArgumentType.string()) { userArg ->
                    hints { platformServer.getOnlinePlayers().map(OnlineKPlayer::name) }
                    runs(commandExceptionHandler::handle) { ctx ->
                        val offlinePlayer = ctx.requireArgument(userArg, KPlayerArgumentConverter(platformServer))
                        LinkCommandExecutor.Intent.UserInfo(
                            targetPlayerUuid = offlinePlayer.uuid,
                            sender = ctx.requirePlayer()
                        ).run(::launchIntent)
                    }
                }
                runs(commandExceptionHandler::handle) { ctx ->
                    val player = ctx.requirePlayer()
                    LinkCommandExecutor.Intent.Link(player).run(::launchIntent)
                }
            }
        }
    }
}
