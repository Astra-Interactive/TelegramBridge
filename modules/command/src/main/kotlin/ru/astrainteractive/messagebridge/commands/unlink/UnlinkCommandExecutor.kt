package ru.astrainteractive.messagebridge.commands.unlink

import ru.astrainteractive.astralibs.localization.component.LocalizableComponent
import ru.astrainteractive.astralibs.server.KAudience
import ru.astrainteractive.astralibs.server.player.OnlineKPlayer
import ru.astrainteractive.klibs.kstorage.api.CachedKrate
import ru.astrainteractive.klibs.kstorage.api.getValue
import ru.astrainteractive.messagebridge.core.PluginTranslation
import ru.astrainteractive.messagebridge.link.UnlinkResponse
import ru.astrainteractive.messagebridge.link.Unlinking
import java.util.UUID

internal class UnlinkCommandExecutor(
    private val unlinking: Unlinking,
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

    private suspend fun unlink(
        uuid: UUID,
        sender: KAudience,
        notLinkedText: LocalizableComponent,
        unlinkedText: LocalizableComponent
    ) {
        val text = when (unlinking.unlink(uuid)) {
            UnlinkResponse.Unlinked -> unlinkedText
            UnlinkResponse.NotLinked -> notLinkedText
            UnlinkResponse.UnknownError -> translation.commandError.unknownError
        }
        sender.sendMessage(text)
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
