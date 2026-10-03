package ru.astrainteractive.messagebridge.link.command

import ru.astrainteractive.astralibs.localization.component.LocalizableComponent
import ru.astrainteractive.astralibs.server.KAudience
import ru.astrainteractive.astralibs.server.player.OnlineKPlayer
import ru.astrainteractive.klibs.kstorage.api.CachedKrate
import ru.astrainteractive.klibs.kstorage.api.getValue
import ru.astrainteractive.klibs.mikro.core.logging.JUtiltLogger
import ru.astrainteractive.klibs.mikro.core.logging.Logger
import ru.astrainteractive.messagebridge.core.PluginTranslation
import ru.astrainteractive.messagebridge.link.config.LinkTranslation
import ru.astrainteractive.messagebridge.link.controller.LuckPermsRoleController
import ru.astrainteractive.messagebridge.link.database.dao.LinkingDao
import java.util.UUID

internal class UnlinkCommandExecutor(
    private val linkingDao: LinkingDao,
    private val luckPermsRoleController: LuckPermsRoleController,
    translationKrate: CachedKrate<PluginTranslation>,
    linkTranslationKrate: CachedKrate<LinkTranslation>
) : Logger by JUtiltLogger("MessageBridge-UnlinkCommandExecutor") {
    private val translation by translationKrate
    private val linkTranslation by linkTranslationKrate

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
        val existing = linkingDao.findByUuid(uuid).getOrElse { failure ->
            reportFailure(sender, uuid, failure)
            return
        }
        if (existing == null) {
            sender.sendMessage(notLinkedText)
            return
        }
        linkingDao.deleteByUuid(uuid)
            .onSuccess { _ ->
                luckPermsRoleController.removeLinkedRole(uuid)
                sender.sendMessage(unlinkedText)
            }
            .onFailure { failure -> reportFailure(sender, uuid, failure) }
    }

    suspend fun onIntent(intent: Intent) {
        when (intent) {
            is Intent.Unlink -> unlink(
                uuid = intent.player.uuid,
                sender = intent.player,
                notLinkedText = linkTranslation.unlink.notLinked,
                unlinkedText = linkTranslation.unlink.success
            )

            is Intent.AdminUnlink -> unlink(
                uuid = intent.targetPlayerUuid,
                sender = intent.sender,
                notLinkedText = linkTranslation.unlink.playerNotLinked,
                unlinkedText = linkTranslation.unlink.playerSuccess
            )
        }
    }
}
