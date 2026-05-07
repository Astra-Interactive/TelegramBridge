package ru.astrainteractive.messagebridge.commands.link

import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import ru.astrainteractive.astralibs.command.api.argumenttype.KPlayerArgumentConverter
import ru.astrainteractive.astralibs.command.api.brigadier.command.MultiplatformCommand
import ru.astrainteractive.astralibs.server.bridge.PlatformServer
import ru.astrainteractive.astralibs.server.player.OnlineKPlayer

internal class LinkLiteralArgumentBuilder(
    private val executor: LinkCommandExecutor,
    private val multiplatformCommand: MultiplatformCommand,
    private val platformServer: PlatformServer
) {

    fun create(): LiteralArgumentBuilder<Any> {
        return with(multiplatformCommand) {
            command("link") {
                argument("user", StringArgumentType.string()) { userArg ->
                    hints { platformServer.getOnlinePlayers().map(OnlineKPlayer::name) }
                    runs { ctx ->
                        val offlinePlayer = ctx.requireArgument(userArg, KPlayerArgumentConverter(platformServer))
                        LinkCommandExecutor.Intent.UserInfo(
                            targetPlayerUuid = offlinePlayer.uuid,
                            sender = ctx.requirePlayer()
                        ).run(executor::onIntent)
                    }
                }
                runs { ctx ->
                    val player = ctx.requirePlayer()
                    LinkCommandExecutor.Intent.Link(player).run(executor::onIntent)
                }
            }
        }
    }
}
