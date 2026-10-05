package ru.astrainteractive.messagebridge.link.command

import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import ru.astrainteractive.astralibs.command.api.argumenttype.KPlayerArgumentConverter
import ru.astrainteractive.astralibs.command.api.brigadier.command.MultiplatformCommand
import ru.astrainteractive.astralibs.server.bridge.PlatformServer
import ru.astrainteractive.astralibs.server.player.OnlineKPlayer
import ru.astrainteractive.messagebridge.core.command.CommandExceptionHandler
import ru.astrainteractive.messagebridge.link.permission.LinkPermission

internal class UnlinkLiteralArgumentBuilder(
    private val executor: UnlinkCommandExecutor,
    private val ioScope: CoroutineScope,
    private val multiplatformCommand: MultiplatformCommand,
    private val platformServer: PlatformServer,
    private val commandExceptionHandler: CommandExceptionHandler,
) {

    private fun launchIntent(ctx: CommandContext<Any>, intent: UnlinkCommandExecutor.Intent) {
        ioScope.launch(commandExceptionHandler.coroutineExceptionHandler(ctx)) {
            executor.onIntent(intent)
        }
    }

    fun create(): LiteralArgumentBuilder<Any> {
        return with(multiplatformCommand) {
            command("unlink") {
                argument("dao", StringArgumentType.string()) { playerArg ->
                    hints { platformServer.getOnlinePlayers().map(OnlineKPlayer::name) }
                    runs(commandExceptionHandler::handle) { ctx ->
                        ctx.requirePermission(LinkPermission.UnlinkPlayer)
                        val offlinePlayer = ctx.requireArgument(playerArg, KPlayerArgumentConverter(platformServer))
                        val intent = UnlinkCommandExecutor.Intent.AdminUnlink(
                            targetPlayerUuid = offlinePlayer.uuid,
                            sender = ctx.requirePlayer()
                        )
                        launchIntent(ctx, intent)
                    }
                }
                runs(commandExceptionHandler::handle) { ctx ->
                    val player = ctx.requirePlayer()
                    launchIntent(ctx, UnlinkCommandExecutor.Intent.Unlink(player))
                }
            }
        }
    }
}
