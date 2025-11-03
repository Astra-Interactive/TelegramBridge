package ru.astrainteractive.messagebridge.commands.link

import com.mojang.brigadier.arguments.StringArgumentType
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import ru.astrainteractive.astralibs.command.api.registrar.PaperCommandRegistrarContext
import ru.astrainteractive.astralibs.command.api.util.argument
import ru.astrainteractive.astralibs.command.api.util.command
import ru.astrainteractive.astralibs.command.api.util.hints
import ru.astrainteractive.astralibs.command.api.util.requireArgument
import ru.astrainteractive.astralibs.command.api.util.requirePlayer
import ru.astrainteractive.astralibs.command.api.util.runs

internal class LinkCommandRegistry(
    private val commandRegistrarContext: PaperCommandRegistrarContext,
    private val executor: LinkCommandExecutor
) {

    fun register() {
        command("link") {
            argument("user", StringArgumentType.string()) { userArg ->
                hints { Bukkit.getOnlinePlayers().map(Player::getName) }
                runs { ctx ->
                    val offlinePlayer = ctx.requireArgument(userArg, OfflinePlayerArgumentType)
                    LinkCommandExecutor.Intent.UserInfo(
                        targetPlayerUuid = offlinePlayer.uniqueId,
                        sender = ctx.requirePlayer()
                    ).run(executor::onIntent)
                }
            }
            runs { ctx ->
                val player = ctx.requirePlayer()
                LinkCommandExecutor.Intent.Link(player).run(executor::onIntent)
            }
        }.build().run(commandRegistrarContext::registerWhenReady)
    }
}
