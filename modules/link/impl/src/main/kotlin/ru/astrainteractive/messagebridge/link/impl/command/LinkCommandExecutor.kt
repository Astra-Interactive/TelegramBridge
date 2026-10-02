package ru.astrainteractive.messagebridge.link.impl.command

import net.kyori.adventure.text.Component
import ru.astrainteractive.astralibs.server.player.OnlineKPlayer
import ru.astrainteractive.klibs.kstorage.api.CachedKrate
import ru.astrainteractive.klibs.kstorage.api.getValue
import ru.astrainteractive.klibs.mikro.core.logging.JUtiltLogger
import ru.astrainteractive.klibs.mikro.core.logging.Logger
import ru.astrainteractive.messagebridge.core.api.config.PluginTranslation
import ru.astrainteractive.messagebridge.link.api.config.LinkTranslation
import ru.astrainteractive.messagebridge.link.api.player.api.LinkingDao
import ru.astrainteractive.messagebridge.link.impl.code.api.CodeApi
import ru.astrainteractive.messagebridge.link.impl.code.model.CodeUser
import java.util.UUID

internal class LinkCommandExecutor(
    private val codeApi: CodeApi,
    private val linkingDao: LinkingDao,
    translationKrate: CachedKrate<PluginTranslation>,
    linkTranslationKrate: CachedKrate<LinkTranslation>
) : Logger by JUtiltLogger("MessageBridge-LinkCommandExecutor") {
    private val translation by translationKrate
    private val linkTranslation by linkTranslationKrate

    sealed interface Intent {
        data class Link(val player: OnlineKPlayer) : Intent
        data class UserInfo(
            val targetPlayerUuid: UUID,
            val sender: OnlineKPlayer
        ) : Intent
    }

    private suspend fun link(player: OnlineKPlayer) {
        val codeUser = CodeUser(
            name = player.name,
            uuid = player.uuid
        )
        val code = codeApi.generateCodeForPlayer(codeUser)
        player.sendMessage(linkTranslation.link.codeCreated(code))
    }

    private suspend fun showUserInfo(intent: Intent.UserInfo) {
        val user = linkingDao.findByUuid(intent.targetPlayerUuid).getOrElse { t ->
            error(t) { "#showUserInfo could not read the link of ${intent.targetPlayerUuid}" }
            intent.sender.sendMessage(translation.commandError.unknownError)
            return
        }
        if (user == null) {
            intent.sender.sendMessage(linkTranslation.unlink.playerNotLinked)
            return
        }
        intent.sender.sendMessage(
            Component.text(
                "DiscordID: ${user.discordLink?.discordId}; " +
                    "telegramUsername: ${user.telegramLink?.telegramUsername}; " +
                    "minecraftUUID: ${user.uuid}"
            )
        )
    }

    suspend fun onIntent(intent: Intent) {
        when (intent) {
            is Intent.Link -> link(intent.player)
            is Intent.UserInfo -> showUserInfo(intent)
        }
    }
}
