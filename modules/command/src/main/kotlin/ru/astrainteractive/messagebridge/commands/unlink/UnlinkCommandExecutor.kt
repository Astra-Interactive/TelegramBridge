package ru.astrainteractive.messagebridge.commands.unlink

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import ru.astrainteractive.astralibs.kyori.KyoriComponentSerializer
import ru.astrainteractive.astralibs.kyori.unwrap
import ru.astrainteractive.astralibs.server.player.OnlineKPlayer
import ru.astrainteractive.klibs.kstorage.api.CachedKrate
import ru.astrainteractive.klibs.kstorage.api.getValue
import ru.astrainteractive.messagebridge.core.PluginTranslation
import ru.astrainteractive.messagebridge.link.database.dao.LinkingDao
import java.util.UUID

internal class UnlinkCommandExecutor(
    private val ioScope: CoroutineScope,
    private val linkingDao: LinkingDao,
    translationKrate: CachedKrate<PluginTranslation>,
    kyoriKrate: CachedKrate<KyoriComponentSerializer>,
) : KyoriComponentSerializer by kyoriKrate.unwrap() {
    private val translation by translationKrate

    sealed interface Intent {
        data class Unlink(val player: OnlineKPlayer) : Intent
        data class AdminUnlink(
            val targetPlayerUuid: UUID,
            val sender: OnlineKPlayer,
        ) : Intent
    }

    fun onIntent(intent: Intent) {
        when (intent) {
            is Intent.Unlink -> {
                ioScope.launch {
                    val player = intent.player
                    val existing = linkingDao.findByUuid(player.uuid).getOrNull()
                    if (existing == null) {
                        player.sendMessage(translation.unlink.notLinked.component)
                        return@launch
                    }
                    val result = linkingDao.deleteByUuid(player.uuid)
                    if (result.isSuccess) {
                        player.sendMessage(translation.unlink.success.component)
                    } else {
                        player.sendMessage(translation.link.unknownError.component)
                    }
                }
            }

            is Intent.AdminUnlink -> {
                ioScope.launch {
                    val existing = linkingDao.findByUuid(intent.targetPlayerUuid).getOrNull()
                    if (existing == null) {
                        intent.sender.sendMessage(translation.unlink.playerNotLinked.component)
                        return@launch
                    }
                    val result = linkingDao.deleteByUuid(intent.targetPlayerUuid)
                    if (result.isSuccess) {
                        intent.sender.sendMessage(translation.unlink.playerUnlinkSuccess.component)
                    } else {
                        intent.sender.sendMessage(translation.link.unknownError.component)
                    }
                }
            }
        }
    }
}
