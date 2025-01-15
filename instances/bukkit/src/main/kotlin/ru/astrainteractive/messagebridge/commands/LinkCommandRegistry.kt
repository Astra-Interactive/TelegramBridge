package ru.astrainteractive.messagebridge.commands

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import ru.astrainteractive.astralibs.command.api.argumenttype.ArgumentType
import ru.astrainteractive.astralibs.command.api.context.BukkitCommandContext
import ru.astrainteractive.astralibs.command.api.context.BukkitCommandContextExt.requireArgument
import ru.astrainteractive.astralibs.command.api.exception.CommandException
import ru.astrainteractive.astralibs.kyori.KyoriComponentSerializer
import ru.astrainteractive.klibs.kstorage.api.Krate
import ru.astrainteractive.messagebridge.core.LifecyclePlugin
import ru.astrainteractive.messagebridge.core.PluginTranslation
import ru.astrainteractive.messagebridge.core.util.getValue
import ru.astrainteractive.messagebridge.link.api.CodeApi
import ru.astrainteractive.messagebridge.link.api.model.CodeUser
import ru.astrainteractive.messagebridge.link.database.dao.LinkingDao

internal class LinkCommandRegistry(
    translationKrate: Krate<PluginTranslation>,
    kyoriKrate: Krate<KyoriComponentSerializer>,
    private val plugin: LifecyclePlugin,
    private val scope: CoroutineScope,
    private val codeApi: CodeApi,
    private val linkingDao: LinkingDao
) {
    private val translation by translationKrate
    private val kyori by kyoriKrate

    object OfflinePlayerArgumentType : ArgumentType<OfflinePlayer> {
        override val key: String = "OfflinePlayerArgument"

        class PlayerNotFoundException(name: String) : CommandException("Player $name not found")

        override fun transform(value: String): OfflinePlayer {
            val offlinePlayer = Bukkit.getOfflinePlayer(value)
            if (!offlinePlayer.hasPlayedBefore()) throw PlayerNotFoundException(value)
            return offlinePlayer
        }
    }

    private fun register() = plugin.getCommand("link")?.setExecutor { sender, command, label, args ->
        val player = sender as? Player ?: return@setExecutor true
        val commandContext = BukkitCommandContext(
            sender = sender,
            command = command,
            label = label,
            args = args
        )
        scope.launch {
            when {
                args.getOrNull(0) == "user" -> {
                    val offlinePlayer = commandContext.requireArgument(1, OfflinePlayerArgumentType)
                    val user = linkingDao.findByUuid(offlinePlayer.uniqueId).getOrNull()
                    if (user == null) {
                        sender.sendMessage("User not found")
                    } else {
                        @Suppress("MaximumLineLength", "MaxLineLength")
                        sender.sendMessage(
                            "DiscordID: ${user.discordLink?.discordId}; telegramUsername: ${user.telegramLink?.telegramUsername}; minecraftUUID: ${user.uuid}"
                        )
                    }
                }

                else -> {
                    val codeUser = CodeUser(
                        name = player.name,
                        uuid = player.uniqueId
                    )
                    val code = codeApi.generateCodeForPlayer(codeUser)
                    with(kyori) {
                        translation.link.codeCreated(code).component
                            .let(KyoriComponentSerializer.Plain.serializer::serialize)
                            .run(player::sendMessage)
                    }
                }
            }
        }
        true
    }

    init {
        register()
    }
}
