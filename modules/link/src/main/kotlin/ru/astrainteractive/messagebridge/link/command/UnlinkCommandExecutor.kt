package ru.astrainteractive.messagebridge.link.command

import ru.astrainteractive.astralibs.localization.component.LocalizableComponent
import ru.astrainteractive.astralibs.server.KAudience
import ru.astrainteractive.astralibs.server.player.OnlineKPlayer
import ru.astrainteractive.klibs.kstorage.api.CachedKrate
import ru.astrainteractive.klibs.kstorage.api.getValue
import ru.astrainteractive.klibs.mikro.core.logging.JUtiltLogger
import ru.astrainteractive.klibs.mikro.core.logging.Logger
import ru.astrainteractive.messagebridge.core.config.PluginTranslation
import ru.astrainteractive.messagebridge.link.internal.DiscordRoleController
import ru.astrainteractive.messagebridge.link.internal.LuckPermsRoleController
import ru.astrainteractive.messagebridge.link.dao.api.LinkingDao
import java.util.UUID

internal class UnlinkCommandExecutor(
    private val linkingDao: LinkingDao,
    private val luckPermsRoleController: LuckPermsRoleController,
    private val discordRoleController: DiscordRoleController,
    translationKrate: CachedKrate<PluginTranslation>
) : Logger by JUtiltLogger("MessageBridge-UnlinkCommandExecutor") {
    private val translation by translationKrate

    sealed interface Intent {
        data class Unlink(val player: OnlineKPlayer) : Intent
        data class AdminUnlink(
            val targetPlayerUuid: UUID,
            val sender: OnlineKPlayer,
        ) : Intent
    }

    private fun reportFailure(sender: KAudience, uuid: UUID, failure: Throwable) {
        error(failure) { "#unlink could not unlink $uuid" }
        sender.sendMessage(translation.commandError.unknownError)
    }

    private suspend fun unlink(
        uuid: UUID,
        sender: KAudience,
        notLinkedText: LocalizableComponent,
        unlinkedText: LocalizableComponent
    ) {
        val unlinked = linkingDao.deleteByUuid(uuid).getOrElse { t ->
            reportFailure(sender, uuid, t)
            return
        }
        luckPermsRoleController.removeLinkedRole(uuid)
        unlinked?.discord?.let { discord -> discordRoleController.removeLinkedRole(discord.id) }
        if (unlinked == null) {
            sender.sendMessage(notLinkedText)
            return
        }
        sender.sendMessage(unlinkedText)
    }

    suspend fun onIntent(intent: Intent) {
        when (intent) {
            is Intent.Unlink -> unlink(
                uuid = intent.player.uuid,
                sender = intent.player,
                notLinkedText = translation.unlink.notLinked,
                unlinkedText = translation.unlink.success
            )

            is Intent.AdminUnlink -> unlink(
                uuid = intent.targetPlayerUuid,
                sender = intent.sender,
                notLinkedText = translation.unlink.playerNotLinked,
                unlinkedText = translation.unlink.playerSuccess
            )
        }
    }
}
