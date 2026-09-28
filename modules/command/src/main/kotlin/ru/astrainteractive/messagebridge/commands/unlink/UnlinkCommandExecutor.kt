package ru.astrainteractive.messagebridge.commands.unlink

import ru.astrainteractive.astralibs.server.player.OnlineKPlayer
import ru.astrainteractive.klibs.kstorage.api.CachedKrate
import ru.astrainteractive.klibs.kstorage.api.getValue
import ru.astrainteractive.messagebridge.core.PluginTranslation
import ru.astrainteractive.messagebridge.link.database.dao.LinkingDao
import java.util.UUID

internal class UnlinkCommandExecutor(
    private val linkingDao: LinkingDao,
    translationKrate: CachedKrate<PluginTranslation>
) {
    private val translation by translationKrate

    sealed interface Intent {
        data class Unlink(val player: OnlineKPlayer) : Intent
        data class AdminUnlink(
            val targetPlayerUuid: UUID,
            val sender: OnlineKPlayer,
        ) : Intent
    }

    suspend fun onIntent(intent: Intent) {
        when (intent) {
            is Intent.Unlink -> {
                val player = intent.player
                val existing = linkingDao.findByUuid(player.uuid).getOrNull()
                if (existing == null) {
                    player.sendMessage(translation.unlink.notLinked)
                    return
                }
                val result = linkingDao.deleteByUuid(player.uuid)
                if (result.isSuccess) {
                    player.sendMessage(translation.unlink.success)
                } else {
                    player.sendMessage(translation.link.unknownError)
                }
            }

            is Intent.AdminUnlink -> {
                val existing = linkingDao.findByUuid(intent.targetPlayerUuid).getOrNull()
                if (existing == null) {
                    intent.sender.sendMessage(translation.unlink.playerNotLinked)
                    return
                }
                val result = linkingDao.deleteByUuid(intent.targetPlayerUuid)
                if (result.isSuccess) {
                    intent.sender.sendMessage(translation.unlink.playerSuccess)
                } else {
                    intent.sender.sendMessage(translation.link.unknownError)
                }
            }
        }
    }
}
