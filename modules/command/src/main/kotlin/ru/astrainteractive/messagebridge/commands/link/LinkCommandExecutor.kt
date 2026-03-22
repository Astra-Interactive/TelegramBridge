package ru.astrainteractive.messagebridge.commands.link

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import net.kyori.adventure.text.Component
import ru.astrainteractive.astralibs.kyori.KyoriComponentSerializer
import ru.astrainteractive.astralibs.server.player.OnlineKPlayer
import ru.astrainteractive.klibs.kstorage.api.CachedKrate
import ru.astrainteractive.klibs.kstorage.util.getValue
import ru.astrainteractive.messagebridge.core.PluginTranslation
import ru.astrainteractive.messagebridge.link.api.CodeApi
import ru.astrainteractive.messagebridge.link.api.model.CodeUser
import ru.astrainteractive.messagebridge.link.database.dao.LinkingDao
import java.util.UUID

internal class LinkCommandExecutor(
    private val ioScope: CoroutineScope,
    private val codeApi: CodeApi,
    private val linkingDao: LinkingDao,
    translationKrate: CachedKrate<PluginTranslation>,
    kyoriKrate: CachedKrate<KyoriComponentSerializer>,
) {
    private val kyori by kyoriKrate
    private val translation by translationKrate

    sealed interface Intent {
        data class Link(val player: OnlineKPlayer) : Intent
        data class UserInfo(
            val targetPlayerUuid: UUID,
            val sender: OnlineKPlayer
        ) : Intent
    }

    fun onIntent(intent: Intent) {
        when (intent) {
            is Intent.Link -> {
                ioScope.launch {
                    val player = intent.player
                    val codeUser = CodeUser(
                        name = player.name,
                        uuid = player.uuid
                    )
                    val code = codeApi.generateCodeForPlayer(codeUser)
                    with(kyori) {
                        player.sendMessage(translation.link.codeCreated(code).component)
                    }
                }
            }

            is Intent.UserInfo -> {
                ioScope.launch {
                    val user = linkingDao.findByUuid(intent.targetPlayerUuid).getOrNull()
                    if (user == null) {
                        intent.sender.sendMessage(Component.text("User not found"))
                    } else {
                        intent.sender.sendMessage(
                            Component.text(
                                "DiscordID: ${user.discordLink?.discordId}; " +
                                    "telegramUsername: ${user.telegramLink?.telegramUsername}; " +
                                    "minecraftUUID: ${user.uuid}"
                            )
                        )
                    }
                }
            }
        }
    }
}
