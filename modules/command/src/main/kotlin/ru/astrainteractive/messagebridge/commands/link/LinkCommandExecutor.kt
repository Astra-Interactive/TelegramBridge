package ru.astrainteractive.messagebridge.commands.link

import net.kyori.adventure.text.Component
import ru.astrainteractive.astralibs.server.player.OnlineKPlayer
import ru.astrainteractive.klibs.kstorage.api.CachedKrate
import ru.astrainteractive.klibs.kstorage.api.getValue
import ru.astrainteractive.messagebridge.core.PluginTranslation
import ru.astrainteractive.messagebridge.link.api.CodeApi
import ru.astrainteractive.messagebridge.link.api.model.CodeUser
import ru.astrainteractive.messagebridge.link.database.dao.LinkingDao
import java.util.UUID

internal class LinkCommandExecutor(
    private val codeApi: CodeApi,
    private val linkingDao: LinkingDao,
    translationKrate: CachedKrate<PluginTranslation>
) {
    private val translation by translationKrate

    sealed interface Intent {
        data class Link(val player: OnlineKPlayer) : Intent
        data class UserInfo(
            val targetPlayerUuid: UUID,
            val sender: OnlineKPlayer
        ) : Intent
    }

    suspend fun onIntent(intent: Intent) {
        when (intent) {
            is Intent.Link -> {
                val player = intent.player
                val codeUser = CodeUser(
                    name = player.name,
                    uuid = player.uuid
                )
                val code = codeApi.generateCodeForPlayer(codeUser)
                player.sendMessage(translation.link.codeCreated(code))
            }

            is Intent.UserInfo -> {
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
